package com.izuuhh.mira

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.spotify.protocol.types.Repeat
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class MiraViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)
    private val http = OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build()
    private val auth = Auth(http, prefs, TokenStore(app))
    private val api = SpotifyApi(http, auth)
    private val remote = RemotePlayer(app) { onRemoteState(it) }
    private var remoteFailed = false

    /** App Remote connects with the Activity as context, held weakly so the ViewModel cannot leak it. */
    private var hostRef = java.lang.ref.WeakReference<android.app.Activity>(null)
    fun attachHost(activity: android.app.Activity?) { hostRef = java.lang.ref.WeakReference(activity) }

    var connected by mutableStateOf(auth.hasSession); private set
    var authMessage by mutableStateOf(""); private set
    var signingIn by mutableStateOf(false); private set
    var page by mutableStateOf(Page.Home); private set
    var busy by mutableStateOf(false); private set
    var info by mutableStateOf(""); private set
    var loadError by mutableStateOf(false); private set
    var items by mutableStateOf<List<Track>>(emptyList()); private set
    var playlists by mutableStateOf<List<Track>>(emptyList()); private set
    var selectedPlaylist by mutableStateOf<Track?>(null); private set
    var devices by mutableStateOf<List<Device>>(emptyList()); private set
    var playback by mutableStateOf<Playback?>(null); private set
    var deviceId by mutableStateOf(""); private set
    var searchTerm by mutableStateOf("")
    var online by mutableStateOf(true); private set
    var lyricsEnabled by mutableStateOf(prefs.lyrics); private set
    var lyrics by mutableStateOf<LyricsState>(LyricsState.Idle); private set
    private val lyricsClient = LyricsClient(http)
    private var lyricsUri: String? = null

    private val events = Channel<Event>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    private var nextUrl: String? = null
    private var pollJob: Job? = null
    private var pendingPlay: (() -> Unit)? = null
    private var pendingExpiry: Job? = null
    private var userId: String? = null

    // --- Phone volume ----------------------------------------------------------------------------

    private val audio = app.getSystemService(android.media.AudioManager::class.java)

    /** The phone's own media volume, 0..100. Used when Spotify cannot be volume-controlled remotely. */
    var systemVolume by mutableFloatStateOf(readSystemVolume()); private set

    private fun readSystemVolume(): Float {
        val max = audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) * 100f / max
    }

    fun refreshSystemVolume() { systemVolume = readSystemVolume() }

    fun changeSystemVolume(percent: Float) {
        val max = audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, (percent / 100f * max).roundToInt().coerceIn(0, max), 0)
        systemVolume = readSystemVolume()
    }

    init {
        CloseHook.pause = { remote.player?.pause() }
        CloseHook.disconnect = { remote.disconnect() }
        // The volume buttons on the phone move the slider too.
        androidx.core.content.ContextCompat.registerReceiver(
            app,
            object : android.content.BroadcastReceiver() {
                override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) { refreshSystemVolume() }
            },
            android.content.IntentFilter("android.media.VOLUME_CHANGED_ACTION"),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        watchNetwork()
        if (connected) onConnected()
    }

    /** Tracks whether the phone has internet, so the app can say so and recover by itself. */
    private fun watchNetwork() {
        val cm = getApplication<Application>().getSystemService(android.net.ConnectivityManager::class.java) ?: return
        online = cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        cm.registerDefaultNetworkCallback(object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                val wasOffline = !online
                online = true
                if (wasOffline && connected) {
                    refreshPlayer(true)
                    if (loadError) retry()
                }
            }

            override fun onLost(network: android.net.Network) { online = false }
        })
    }

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
        if (!signingIn) return@launch
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
        remote.disconnect()
        connected = false; signingIn = false; authMessage = "Not connected"
        items = emptyList(); playlists = emptyList(); devices = emptyList(); playback = null; deviceId = ""; userId = null
        page = Page.Settings
    }

    private fun onConnected() {
        remoteFailed = false
        connectRemote()
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
        Page.Album -> { val back = lastSearchRun; if (back != null) back() else page = Page.Home; true }
        Page.Home -> false
        else -> { page = Page.Home; true }
    }

    // --- Search --------------------------------------------------------------------------------

    var searchArtists by mutableStateOf<List<Track>>(emptyList()); private set
    var searchAlbums by mutableStateOf<List<Track>>(emptyList()); private set
    var searchPlaylists by mutableStateOf<List<Track>>(emptyList()); private set
    var recents by mutableStateOf(prefs.recentSearches); private set
    var selectedAlbum by mutableStateOf<Track?>(null); private set
    private var searchQuery = ""
    private var lastSearchRun: (() -> Unit)? = null
    private var searchOffset = 0

    /** Searches songs, artists, albums and playlists at once. */
    fun search(term: String) {
        searchTerm = term
        if (term.isBlank()) { page = Page.Search; return }
        rememberSearch(term.trim())
        runSearch(term.trim(), "track,artist,album,playlist")
    }

    /** All songs and albums by one artist. */
    fun searchArtist(artist: Track) {
        searchTerm = artist.name
        runSearch("artist:\"${artist.name}\"", "track,album")
    }

    private fun runSearch(query: String, types: String) {
        page = Page.Search
        searchQuery = query
        lastSearchRun = { runSearch(query, types) }
        searchOffset = 0
        val q = java.net.URLEncoder.encode(query, "UTF-8")
        loadJob?.cancel()
        lastLoad = { runSearch(query, types) }
        loadJob = viewModelScope.launch {
            busy = true; loadError = false
            items = emptyList(); nextUrl = null
            searchArtists = emptyList(); searchAlbums = emptyList(); searchPlaylists = emptyList()
            when (val r = api.call("GET", "/search?q=$q&type=$types&limit=10")) {
                is ApiResult.Err -> { info = r.message; loadError = true }
                is ApiResult.Ok -> {
                    fun list(key: String) = r.json.optJSONObject(key)?.optJSONArray("items")?.let { a ->
                        (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let(SpotifyApi::parseTrack) }
                    } ?: emptyList()
                    items = list("tracks"); searchArtists = list("artists"); searchAlbums = list("albums"); searchPlaylists = list("playlists")
                    searchOffset = items.size
                    hasMoreSongs = r.json.optJSONObject("tracks")?.optString("next").let { !it.isNullOrEmpty() && it != "null" }
                    info = if (items.isEmpty() && searchAlbums.isEmpty() && searchArtists.isEmpty() && searchPlaylists.isEmpty()) "Nothing found for ‘$query’." else ""
                }
            }
            busy = false
        }
    }

    private var hasMoreSongs = false

    private fun loadMoreSongs() {
        viewModelScope.launch {
            busy = true
            val q = java.net.URLEncoder.encode(searchQuery, "UTF-8")
            val r = api.call("GET", "/search?q=$q&type=track&limit=10&offset=$searchOffset")
            if (r is ApiResult.Ok) {
                val tracks = r.json.optJSONObject("tracks")
                val a = tracks?.optJSONArray("items")
                val more = if (a == null) emptyList() else (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let(SpotifyApi::parseTrack) }
                items = items + more
                searchOffset += more.size
                hasMoreSongs = more.isNotEmpty() && tracks?.optString("next").let { !it.isNullOrEmpty() && it != "null" }
            } else if (r is ApiResult.Err) events.trySend(Event.Message(r.message))
            busy = false
        }
    }

    private fun rememberSearch(term: String) {
        val list = listOf(term) + prefs.recentSearches.filterNot { it.equals(term, ignoreCase = true) }
        prefs.recentSearches = list
        recents = prefs.recentSearches
    }

    fun clearRecents() { prefs.recentSearches = emptyList(); recents = emptyList() }

    fun openAlbum(a: Track) {
        if (!a.id.matches(ID)) return
        selectedAlbum = a
        page = Page.Album
        load("/albums/${a.id}/tracks?limit=50", map = { it.copy(cover = a.cover, album = a.name) })
    }

    fun loadLibrary() = load("/me/tracks?limit=50")
    fun loadPlaylists() = load("/me/playlists?limit=50", into = { playlists = it })
    fun openPlaylist(p: Track) {
        if (!p.id.matches(ID)) return
        selectedPlaylist = p
        page = Page.Playlist
        load("/playlists/${p.id}/items?limit=50")
    }

    fun loadMore() {
        if (page == Page.Search) { if (hasMoreSongs && !busy) loadMoreSongs() } else nextUrl?.let { load(it, append = true, map = albumMap()) }
    }
    val hasMore get() = if (page == Page.Search) hasMoreSongs else nextUrl != null

    private fun albumMap(): ((Track) -> Track)? = selectedAlbum?.takeIf { page == Page.Album }?.let { a -> { t: Track -> t.copy(cover = a.cover, album = a.name) } }

    private var loadJob: Job? = null
    private var lastLoad: (() -> Unit)? = null

    /** Tries the last list request again, after an error. */
    fun retry() { lastLoad?.invoke() ?: refreshPlayer() }

    /** Opens a Spotify web link (only open.spotify.com). */
    fun openSpotify(url: String) {
        if (url.startsWith("https://open.spotify.com/")) events.trySend(Event.OpenUrl(url))
    }

    private fun load(path: String, key: String? = null, append: Boolean = false, into: ((List<Track>) -> Unit)? = null, map: ((Track) -> Track)? = null) {
        lastLoad = { load(path, key, append, into, map) }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            busy = true
            if (!append && into == null) { items = emptyList(); nextUrl = null }
            when (val r = api.call("GET", path)) {
                is ApiResult.Err -> { info = r.message; loadError = true; busy = false }
                is ApiResult.Ok -> {
                    val obj = if (key != null) r.json.optJSONObject(key) ?: JSONObject() else r.json
                    val arr = obj.optJSONArray("items") ?: JSONArray()
                    val parsed = (0 until arr.length()).mapNotNull { i ->
                        val o = arr.optJSONObject(i) ?: return@mapNotNull null
                        SpotifyApi.parseTrack(o.optJSONObject("track") ?: o.optJSONObject("item") ?: o)?.let { t -> map?.invoke(t) ?: t }
                    }
                    if (into != null) into(parsed) else {
                        items = if (append) items + parsed else parsed
                        nextUrl = obj.optString("next").takeIf { it.isNotEmpty() && it != "null" }
                    }
                    info = if (parsed.isEmpty() && !append) "No results. Try a different selection." else ""
                    loadError = false
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
                is ApiResult.Ok -> applyWebPlayback(SpotifyApi.parsePlayback(r.json))
                is ApiResult.Err -> if (r.code == 401) sessionExpired(r.message)
            }
            if (includeDevices) refreshDevices()
        }
    }

    private var remoteStateAt = 0L

    /**
     * The Web API lags a few seconds behind the Spotify app and then still names the song that just ended.
     * Shortly after App Remote reported a state, keep its song and position and only take the device details.
     */
    private fun applyWebPlayback(web: Playback?) {
        val cur = playback
        val fresh = SystemClock.elapsedRealtime() - remoteStateAt < 5000
        if (web != null && cur != null && fresh && web.track.uri != cur.track.uri) {
            playback = cur.copy(
                deviceId = web.deviceId, deviceName = web.deviceName, volume = web.volume,
                supportsVolume = web.supportsVolume, restricted = web.restricted, disallows = web.disallows,
            )
        } else playback = web
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

    /**
     * The Spotify app pushes its state over App Remote, so there is no need to ask the Web API what is playing.
     * The Web API also lags behind: asked right after a song change it still names the old song, so a new song
     * is built from the App Remote state and the Web API only fills in the rest a moment later.
     */
    private fun onRemoteState(s: com.spotify.protocol.types.PlayerState) {
        val cur = playback
        val t = s.track
        val now = SystemClock.elapsedRealtime()
        remoteStateAt = now
        val repeat = when (s.playbackOptions.repeatMode) { Repeat.ONE -> "track"; Repeat.ALL -> "context"; else -> "off" }
        if (cur == null || t == null || !t.uri.startsWith("spotify:track:")) {
            // Nothing to build on, or an ad/episode: let the Web API describe it, giving it time to catch up.
            refreshSoon()
            return
        }
        val track = if (cur.track.uri == t.uri) cur.track else {
            val id = t.uri.substringAfterLast(':')
            Track(
                id = id, name = t.name, subtitle = t.artists.joinToString(", ") { it.name }, album = t.album?.name.orEmpty(),
                uri = t.uri, cover = t.imageUri?.raw?.substringAfterLast(':')?.takeIf { it.isNotEmpty() }?.let { "https://i.scdn.co/image/$it" },
                type = "track", durationMs = t.duration, url = "https://open.spotify.com/track/$id",
            )
        }
        playback = cur.copy(track = track, playing = !s.isPaused, progressMs = s.playbackPosition, readAt = now, shuffle = s.playbackOptions.isShuffling, repeat = repeat)
        if (cur.track.uri != t.uri) refreshSoon()
    }

    private fun refreshSoon() = viewModelScope.launch { delay(800); refreshPlayer(false) }

    private val controlDevice get() = playback?.deviceId?.takeIf { it.isNotEmpty() } ?: deviceId

    private fun target(path: String, vararg query: Pair<String, String>, device: String = controlDevice): String {
        val q = buildList { query.forEach { add("${it.first}=${java.net.URLEncoder.encode(it.second, "UTF-8")}") }; if (device.isNotEmpty()) add("device_id=$device") }
        return if (q.isEmpty()) path else "$path?${q.joinToString("&")}"
    }

    private fun send(method: String, path: String, body: JSONObject? = null, after: (() -> Unit)? = null) = viewModelScope.launch {
        when (val r = api.call(method, path, body)) {
            // On failure the screen already moved ahead of Spotify, so read the real state back.
            is ApiResult.Err -> { events.trySend(Event.Message(r.message)); refreshSoon() }
            is ApiResult.Ok -> { after?.invoke(); refreshSoon() }
        }
    }

    // --- Spotify App Remote --------------------------------------------------------------------

    private var consentRetry: ((Boolean) -> Unit)? = null

    /**
     * Connects to the Spotify app in the background; [then] runs afterwards with the result.
     * With [userAction], a missing permission opens Spotify's "allow Mira" screen first.
     */
    private fun connectRemote(userAction: Boolean = false, then: ((Boolean) -> Unit)? = null) {
        if (!remote.installed || prefs.clientId.isEmpty()) { remoteFailed = true; then?.invoke(false); return }
        remote.connect(hostRef.get() ?: getApplication(), prefs.clientId, false) { error ->
            when {
                error == null -> { remoteFailed = false; refreshPlayer(true); then?.invoke(true) }
                RemotePlayer.needsConsent(error) && userAction -> {
                    consentRetry = then
                    events.trySend(Event.RequestConsent(prefs.clientId))
                }
                RemotePlayer.needsConsent(error) -> then?.invoke(false)
                else -> { remoteFailed = true; events.trySend(Event.Message(RemotePlayer.describe(error))); then?.invoke(false) }
            }
        }
    }

    /** Called when Spotify's permission screen closes. */
    fun onConsentResult(granted: Boolean) {
        val retry = consentRetry
        consentRetry = null
        if (!granted) { remoteFailed = true; retry?.invoke(false); return }
        connectRemote(userAction = false) { ok -> if (!ok) remoteFailed = true; retry?.invoke(ok) }
    }

    /**
     * Runs [action] through App Remote when it is available, connecting first if needed.
     * Returns false when App Remote cannot be used, so the caller falls back to the Web API.
     */
    private fun viaRemote(retry: () -> Unit, action: (com.spotify.android.appremote.api.PlayerApi) -> Unit): Boolean {
        remote.player?.let { action(it); refreshSoon(); return true }
        if (remoteFailed || !remote.installed) return false
        connectRemote(userAction = true) { retry() }
        return true
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
        remoteFailed = false
        connectRemote(userAction = true)
        events.trySend(Event.OpenSpotifyApp)
        viewModelScope.launch { for (ms in listOf(4000L, 5000L)) { delay(ms); refreshPlayer(true) } }
    }

    private fun startPlayback(body: JSONObject) {
        if (needDevice { startPlayback(body) }) return
        send("PUT", target("/me/player/play", device = deviceId.ifEmpty { controlDevice }), body)
    }

    fun play(uri: String? = null) {
        if (uri != null && !uri.matches(Regex("^spotify:(track|playlist|album|artist):[A-Za-z0-9]+$"))) return
        if (viaRemote({ play(uri) }) { p -> if (uri == null) p.resume() else p.play(uri) }) return
        val b = JSONObject()
        if (uri != null) {
            if (!uri.matches(Regex("^spotify:(track|playlist|album|artist):[A-Za-z0-9]+$"))) return
            if (uri.startsWith("spotify:track:")) b.put("uris", JSONArray().put(uri)) else b.put("context_uri", uri)
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
        if (controlDevice.isEmpty() && viaRemote({ playFrom(uris, index) }) { p -> playList(p, valid.subList(offset, valid.size)) }) return
        startPlayback(JSONObject().put("uris", JSONArray(valid)).put("offset", JSONObject().put("position", offset)))
    }

    /** App Remote plays one item at a time: start the first and queue the next ones. */
    private fun playList(p: com.spotify.android.appremote.api.PlayerApi, tracks: List<String>) {
        p.play(tracks.first()).setResultCallback { queueInOrder(p, tracks.drop(1).take(30)) }
    }

    /** Queues one song at a time: requests sent together can reach Spotify in any order, which scrambled the queue. */
    private fun queueInOrder(p: com.spotify.android.appremote.api.PlayerApi, uris: List<String>) {
        val next = uris.firstOrNull() ?: return
        val rest = uris.drop(1)
        p.queue(next).setResultCallback { queueInOrder(p, rest) }.setErrorCallback { queueInOrder(p, rest) }
    }

    fun playInContext(context: String, uri: String) {
        if (!context.matches(Regex("^spotify:(playlist|album):[A-Za-z0-9]+$"))) return
        val index = items.indexOfFirst { it.uri == uri }
        if (viaRemote({ playInContext(context, uri) }) { p ->
                val start = p.play(context)
                if (index >= 0) start.setResultCallback { p.skipToIndex(context, index) }
            }) return
        val b = JSONObject().put("context_uri", context)
        if (uri.matches(Regex("^spotify:track:[A-Za-z0-9]+$"))) b.put("offset", JSONObject().put("uri", uri))
        startPlayback(b)
    }

    /** Plays the whole Liked songs collection from a track so following tracks keep playing. */
    fun playLiked(uris: List<String>, index: Int, shuffle: Boolean = false) {
        val order = if (shuffle) uris.shuffled() else uris.drop(index)
        val tracks = order.filter { it.matches(Regex("^spotify:track:[A-Za-z0-9]+$")) }
        // Spotify keeps queued songs when a new one starts and App Remote cannot clear them, so queueing a list
        // piles up old batches. Playing a collection by its URI uses Spotify's own order, not the order shown here.
        // With a known device the Web API gets the exact list instead (up to 100 songs, as one fresh context).
        if (controlDevice.isNotEmpty() && tracks.isNotEmpty()) { startPlayback(JSONObject().put("uris", JSONArray(tracks.take(100)))); return }
        if (tracks.isNotEmpty() && viaRemote({ playLiked(uris, index, shuffle) }) { p -> playList(p, tracks) }) return
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

    fun togglePlayback() {
        val p = playback
        if (p?.playing == true) { command("pause"); return }
        // Show "playing" at once; the refresh after the command corrects it if Spotify disagrees.
        if (p != null && p.allows("resuming")) playback = p.copy(playing = true, readAt = SystemClock.elapsedRealtime())
        play()
    }

    fun command(cmd: String) {
        val needs = mapOf("pause" to "pausing", "next" to "skipping_next", "previous" to "skipping_prev")[cmd] ?: return
        if (playback?.allows(needs) == false) { events.trySend(Event.Message("Spotify does not allow this control right now.")); return }
        // Update the screen first so the button answers at once instead of after Spotify's round trip.
        playback?.let { cur ->
            val now = SystemClock.elapsedRealtime()
            when (cmd) {
                "pause" -> playback = cur.copy(progressMs = cur.position(now), playing = false, readAt = now)
                "previous" -> playback = cur.copy(progressMs = 0, readAt = now)
            }
        }
        remote.player?.let { p ->
            when (cmd) { "pause" -> p.pause(); "next" -> p.skipNext(); else -> p.skipPrevious() }
            refreshSoon(); return
        }
        if (controlDevice.isEmpty()) { events.trySend(Event.Message("Choose a Spotify device first.")); return }
        send(if (cmd == "pause") "PUT" else "POST", target("/me/player/$cmd"))
    }

    fun seek(ms: Long) {
        val p = playback ?: return
        if ((controlDevice.isEmpty() && remote.player == null) || !p.allows("seeking")) return
        val clamped = ms.coerceIn(0, if (p.track.durationMs > 0) p.track.durationMs else Long.MAX_VALUE)
        playback = p.copy(progressMs = clamped, readAt = SystemClock.elapsedRealtime())
        remote.player?.let { it.seekTo(clamped); refreshSoon(); return }
        send("PUT", target("/me/player/seek", "position_ms" to clamped.toString()))
    }

    fun setShuffle(on: Boolean) {
        val p = playback ?: return
        if ((controlDevice.isEmpty() && remote.player == null) || !p.allows("toggling_shuffle")) return
        playback = p.copy(shuffle = on)
        remote.player?.let { it.setShuffle(on); refreshSoon(); return }
        send("PUT", target("/me/player/shuffle", "state" to on.toString()))
    }

    fun cycleRepeat() {
        val p = playback ?: return
        val mode = when (p.repeat) { "off" -> "context"; "context" -> "track"; else -> "off" }
        if (controlDevice.isEmpty() && remote.player == null) return
        if (mode != "off" && !p.allows(if (mode == "track") "toggling_repeat_track" else "toggling_repeat_context")) return
        playback = p.copy(repeat = mode)
        remote.player?.let { it.setRepeat(when (mode) { "track" -> Repeat.ONE; "context" -> Repeat.ALL; else -> Repeat.OFF }); refreshSoon(); return }
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

    var closeSpotify by mutableStateOf(prefs.closeSpotify); private set
    fun updateCloseSpotify(v: Boolean) { closeSpotify = v; prefs.closeSpotify = v }
    fun updateLyrics(v: Boolean) { lyricsEnabled = v; prefs.lyrics = v; if (!v) { lyrics = LyricsState.Idle; lyricsUri = null } }

    /** Loads the lyrics of [track] once; asking again for the same song does nothing. */
    fun loadLyrics(track: Track) {
        if (!lyricsEnabled || track.type != "track" || (lyricsUri == track.uri && lyrics !is LyricsState.Failed)) return
        lyricsUri = track.uri
        lyrics = LyricsState.Loading
        viewModelScope.launch {
            val result = lyricsClient.fetch(track.name, track.subtitle.substringBefore(", "), track.album, track.durationMs)
            if (lyricsUri == track.uri) lyrics = result
        }
    }

    private companion object { val ID = Regex("^[A-Za-z0-9]+$") }
}
