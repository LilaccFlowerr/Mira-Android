package com.izuuhh.mira

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.izuuhh.mira.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class Page { Home, Search, Liked, Playlists, Playlist, Settings }

/** One-off things the UI has to do: open a URL, show a snackbar, wake the Spotify app. */
sealed interface Event {
    data class OpenBrowser(val uri: Uri) : Event
    data class Message(val text: String) : Event
    data object OpenSpotifyApp : Event
}

class MiraViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)
    private val http = OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build()
    private val auth = Auth(http, prefs, TokenStore(app))
    private val api = SpotifyApi(http, auth)

    var connected by mutableStateOf(auth.hasSession); private set
    var authMessage by mutableStateOf(""); private set
    var signingIn by mutableStateOf(false); private set
    var page by mutableStateOf(Page.Home); private set
    var busy by mutableStateOf(false); private set
    var info by mutableStateOf(""); private set
    var items by mutableStateOf<List<Track>>(emptyList()); private set
    var playlists by mutableStateOf<List<Track>>(emptyList()); private set
    var selectedPlaylist by mutableStateOf<Track?>(null); private set
    var devices by mutableStateOf<List<Device>>(emptyList()); private set
    var playback by mutableStateOf<Playback?>(null); private set
    var deviceId by mutableStateOf(""); private set
    var searchTerm by mutableStateOf("")
    var dark by mutableStateOf(prefs.dark); private set
    var accent by mutableStateOf(prefs.accent); private set

    private val events = Channel<Event>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    private var nextUrl: String? = null
    private var pollJob: Job? = null
    private var pendingPlay: (() -> Unit)? = null
    private var pendingExpiry: Job? = null
    private var userId: String? = null

    init { if (connected) onConnected() }

    // --- Sign-in -------------------------------------------------------------------------------

    fun setClientId(id: String) { prefs.clientId = id }

    fun login() {
        if (signingIn || connected) return
        val uri = auth.startLogin()
        if (uri == null) { authMessage = "Enter a valid Spotify Client ID (32 characters)."; return }
        signingIn = true
        authMessage = "Finish granting access in your browser…"
        events.trySend(Event.OpenBrowser(uri))
    }

    fun onCallback(uri: Uri) = viewModelScope.launch {
        val error = auth.finishLogin(uri)
        signingIn = false
        if (error != null) { authMessage = error; connected = auth.hasSession; return@launch }
        authMessage = "Connected to Spotify"
        connected = true
        onConnected()
    }

    fun logout() {
        auth.logout()
        pollJob?.cancel()
        connected = false; signingIn = false; authMessage = "Not connected"
        items = emptyList(); playlists = emptyList(); devices = emptyList(); playback = null; deviceId = ""; userId = null
        page = Page.Settings
    }

    private fun onConnected() {
        loadPlaylists()
        refreshPlayer(true)
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            var tick = 0
            while (true) {
                delay(5000)
                tick++
                refreshPlayer(includeDevices = tick % 6 == 0)
            }
        }
    }

    // --- Navigation and content ----------------------------------------------------------------

    fun navigate(target: Page) {
        page = target
        if (!connected) return
        when (target) {
            Page.Liked -> loadLibrary()
            Page.Playlists -> loadPlaylists()
            else -> {}
        }
    }

    /** Back button: returns true when the app handled it. */
    fun back(): Boolean = when (page) {
        Page.Playlist -> { navigate(Page.Playlists); true }
        Page.Home -> false
        else -> { page = Page.Home; true }
    }

    fun search(term: String) {
        searchTerm = term
        if (term.isBlank()) return
        page = Page.Search
        val q = java.net.URLEncoder.encode(term.trim(), "UTF-8")
        load("/search?q=$q&type=track&limit=20", key = "tracks")
    }

    fun loadLibrary() = load("/me/tracks?limit=50")
    fun loadPlaylists() = load("/me/playlists?limit=50", into = { playlists = it })
    fun openPlaylist(p: Track) {
        if (!p.id.matches(ID)) return
        selectedPlaylist = p
        page = Page.Playlist
        load("/playlists/${p.id}/items?limit=50")
    }

    fun loadMore() { nextUrl?.let { load(it, append = true) } }
    val hasMore get() = nextUrl != null

    private var loadJob: Job? = null
    private fun load(path: String, key: String? = null, append: Boolean = false, into: ((List<Track>) -> Unit)? = null) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            busy = true
            if (!append && into == null) { items = emptyList(); nextUrl = null }
            when (val r = api.call("GET", path)) {
                is ApiResult.Err -> { info = r.message; busy = false }
                is ApiResult.Ok -> {
                    val obj = if (key != null) r.json.optJSONObject(key) ?: JSONObject() else r.json
                    val arr = obj.optJSONArray("items") ?: JSONArray()
                    val parsed = (0 until arr.length()).mapNotNull { i ->
                        val o = arr.optJSONObject(i) ?: return@mapNotNull null
                        SpotifyApi.parseTrack(o.optJSONObject("track") ?: o.optJSONObject("item") ?: o)
                    }
                    if (into != null) into(parsed) else {
                        items = if (append) items + parsed else parsed
                        nextUrl = obj.optString("next").takeIf { it.isNotEmpty() && it != "null" }
                    }
                    info = if (parsed.isEmpty() && !append) "No results. Try a different selection." else ""
                    busy = false
                }
            }
        }
    }

    // --- Playback state ------------------------------------------------------------------------

    fun refreshPlayer(includeDevices: Boolean = true) {
        if (!connected) return
        viewModelScope.launch {
            when (val r = api.call("GET", "/me/player")) {
                is ApiResult.Ok -> playback = SpotifyApi.parsePlayback(r.json)
                is ApiResult.Err -> if (r.code == 401) sessionExpired(r.message)
            }
            if (includeDevices) refreshDevices()
        }
    }

    private fun sessionExpired(message: String) {
        auth.logout(); pollJob?.cancel(); connected = false; authMessage = message; page = Page.Settings
    }

    private suspend fun refreshDevices() {
        val r = api.call("GET", "/me/player/devices") as? ApiResult.Ok ?: return
        val arr = r.json.optJSONArray("devices") ?: JSONArray()
        devices = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
            .map(SpotifyApi::parseDevice).filter { it.id.isNotEmpty() }
        if (devices.none { it.id == deviceId }) {
            // On a phone, play on the phone: prefer its Spotify app, the active one if there are several.
            val phones = devices.filter { it.type.equals("Smartphone", true) }
            deviceId = (phones.firstOrNull { it.active } ?: phones.firstOrNull())?.id.orEmpty()
        }
        val run = pendingPlay
        if (run != null && deviceId.isNotEmpty()) { pendingPlay = null; pendingExpiry?.cancel(); run() }
    }

    fun selectDevice(id: String) {
        deviceId = id
        if (playback?.playing == true && playback?.deviceId != id) {
            viewModelScope.launch {
                api.call("PUT", "/me/player", JSONObject().put("device_ids", JSONArray().put(id)).put("play", true))
                refreshSoon()
            }
        }
    }

    private fun refreshSoon() = viewModelScope.launch { delay(800); refreshPlayer(false) }

    private val controlDevice get() = playback?.deviceId?.takeIf { it.isNotEmpty() } ?: deviceId

    private fun target(path: String, vararg query: Pair<String, String>, device: String = controlDevice): String {
        val q = buildList { query.forEach { add("${it.first}=${java.net.URLEncoder.encode(it.second, "UTF-8")}") }; if (device.isNotEmpty()) add("device_id=$device") }
        return if (q.isEmpty()) path else "$path?${q.joinToString("&")}"
    }

    private fun send(method: String, path: String, body: JSONObject? = null, after: (() -> Unit)? = null) = viewModelScope.launch {
        when (val r = api.call(method, path, body)) {
            is ApiResult.Err -> events.trySend(Event.Message(r.message))
            is ApiResult.Ok -> { after?.invoke(); refreshSoon() }
        }
    }

    // --- Controls ------------------------------------------------------------------------------

    /** Phone without an active Spotify device: wake the Spotify app, then run [retry] once it shows up. */
    private fun needDevice(retry: () -> Unit): Boolean {
        if (deviceId.isNotEmpty() || controlDevice.isNotEmpty()) return false
        pendingPlay = retry
        pendingExpiry?.cancel()
        pendingExpiry = viewModelScope.launch { delay(60_000); pendingPlay = null }
        events.trySend(Event.Message("Opening Spotify on this phone. Switch back to Mira and your music starts."))
        events.trySend(Event.OpenSpotifyApp)
        viewModelScope.launch { for (ms in listOf(4000L, 5000L, 6000L)) { delay(ms); refreshPlayer(true) } }
        return true
    }

    fun openSpotifyApp() {
        events.trySend(Event.OpenSpotifyApp)
        viewModelScope.launch { for (ms in listOf(4000L, 5000L)) { delay(ms); refreshPlayer(true) } }
    }

    private fun startPlayback(body: JSONObject) {
        if (needDevice { startPlayback(body) }) return
        send("PUT", target("/me/player/play", device = deviceId.ifEmpty { controlDevice }), body)
    }

    fun play(uri: String? = null) {
        if (needDevice { play(uri) }) return
        val b = JSONObject()
        if (uri != null) {
            if (!uri.matches(Regex("^spotify:(track|playlist):[A-Za-z0-9]+$"))) return
            if (uri.startsWith("spotify:playlist:")) b.put("context_uri", uri) else b.put("uris", JSONArray().put(uri))
        } else if (playback?.allows("resuming") == false) {
            events.trySend(Event.Message("Spotify does not allow resuming right now.")); return
        }
        startPlayback(b)
    }

    fun playFrom(uris: List<String>, index: Int) {
        val track = Regex("^spotify:track:[A-Za-z0-9]+$")
        var offset = 0
        var valid = ArrayList<String>()
        uris.forEachIndexed { i, u -> if (i == index) offset = valid.size; if (track.matches(u)) valid.add(u) }
        if (valid.isEmpty()) return
        offset = minOf(offset, valid.size - 1)
        if (valid.size > 100) { valid = ArrayList(valid.subList(offset, minOf(valid.size, offset + 100))); offset = 0 }
        startPlayback(JSONObject().put("uris", JSONArray(valid)).put("offset", JSONObject().put("position", offset)))
    }

    fun playInContext(context: String, uri: String) {
        if (!context.matches(Regex("^spotify:playlist:[A-Za-z0-9]+$"))) return
        val b = JSONObject().put("context_uri", context)
        if (uri.matches(Regex("^spotify:track:[A-Za-z0-9]+$"))) b.put("offset", JSONObject().put("uri", uri))
        startPlayback(b)
    }

    /** Plays the whole Liked songs collection from a track so following tracks keep playing. */
    fun playLiked(uris: List<String>, index: Int, shuffle: Boolean = false) {
        if (needDevice { playLiked(uris, index, shuffle) }) return
        viewModelScope.launch {
            val id = userId ?: (api.call("GET", "/me") as? ApiResult.Ok)?.json?.optString("id")
                ?.takeIf { it.matches(Regex("^[A-Za-z0-9._-]{1,128}$")) }?.also { userId = it }
            if (id == null) { playFrom(if (shuffle) uris.shuffled() else uris, if (shuffle) 0 else index); return@launch }
            val device = deviceId.ifEmpty { controlDevice }
            val b = JSONObject().put("context_uri", "spotify:user:$id:collection")
            val uri = uris.getOrNull(index).orEmpty()
            if (!shuffle && uri.matches(Regex("^spotify:track:[A-Za-z0-9]+$"))) b.put("offset", JSONObject().put("uri", uri))
            // Shuffle has to be set first so the first song is random too; it fails harmlessly when nothing is active.
            if (shuffle) api.call("PUT", target("/me/player/shuffle", "state" to "true", device = device))
            val r = api.call("PUT", target("/me/player/play", device = device), b)
            if (r is ApiResult.Err) playFrom(if (shuffle) uris.shuffled() else uris, if (shuffle) 0 else index) else refreshSoon()
        }
    }

    fun togglePlayback() { if (playback?.playing == true) command("pause") else play() }

    fun command(cmd: String) {
        val needs = mapOf("pause" to "pausing", "next" to "skipping_next", "previous" to "skipping_prev")[cmd] ?: return
        if (playback?.allows(needs) == false) { events.trySend(Event.Message("Spotify does not allow this control right now.")); return }
        if (controlDevice.isEmpty()) { events.trySend(Event.Message("Choose a Spotify device first.")); return }
        send(if (cmd == "pause") "PUT" else "POST", target("/me/player/$cmd"))
    }

    fun seek(ms: Long) {
        val p = playback ?: return
        if (controlDevice.isEmpty() || !p.allows("seeking")) return
        val clamped = ms.coerceIn(0, if (p.track.durationMs > 0) p.track.durationMs else Long.MAX_VALUE)
        playback = p.copy(progressMs = clamped, readAt = SystemClock.elapsedRealtime())
        send("PUT", target("/me/player/seek", "position_ms" to clamped.toString()))
    }

    fun setShuffle(on: Boolean) {
        val p = playback ?: return
        if (controlDevice.isEmpty() || !p.allows("toggling_shuffle")) return
        playback = p.copy(shuffle = on)
        send("PUT", target("/me/player/shuffle", "state" to on.toString()))
    }

    fun cycleRepeat() {
        val p = playback ?: return
        val mode = when (p.repeat) { "off" -> "context"; "context" -> "track"; else -> "off" }
        if (controlDevice.isEmpty()) return
        if (mode != "off" && !p.allows(if (mode == "track") "toggling_repeat_track" else "toggling_repeat_context")) return
        playback = p.copy(repeat = mode)
        send("PUT", target("/me/player/repeat", "state" to mode))
    }

    fun setVolume(percent: Int) {
        val p = playback ?: return
        if (controlDevice.isEmpty() || !p.supportsVolume) return
        val v = percent.coerceIn(0, 100)
        playback = p.copy(volume = v)
        viewModelScope.launch { api.call("PUT", target("/me/player/volume", "volume_percent" to v.toString())) }
    }

    fun save(uri: String, remove: Boolean = false) {
        if (!uri.matches(Regex("^spotify:track:[A-Za-z0-9]+$"))) return
        send(if (remove) "DELETE" else "PUT", "/me/library?uris=$uri") {
            events.trySend(Event.Message(if (remove) "Removed from your saved songs." else "Added to your saved songs in Spotify."))
        }
    }

    fun queue(uri: String) {
        if (!uri.matches(Regex("^spotify:track:[A-Za-z0-9]+$"))) return
        if (controlDevice.isEmpty()) { events.trySend(Event.Message("Start playback on a device first, then add songs to the queue.")); return }
        send("POST", target("/me/player/queue", "uri" to uri)) { events.trySend(Event.Message("Added to queue.")) }
    }

    // --- Settings ------------------------------------------------------------------------------

    fun updateDark(v: Boolean) { dark = v; prefs.dark = v }
    fun updateAccent(v: Int) { accent = v; prefs.accent = v }

    private companion object { val ID = Regex("^[A-Za-z0-9]+$") }
}
