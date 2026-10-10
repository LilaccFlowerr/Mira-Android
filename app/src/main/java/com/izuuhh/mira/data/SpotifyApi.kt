package com.izuuhh.mira.data

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

sealed interface ApiResult {
    /** [json] is empty for 204 responses. */
    data class Ok(val json: JSONObject) : ApiResult
    data class Err(val code: Int, val message: String) : ApiResult
}

/** Thin Spotify Web API client: auth header, one retry after a 401, and a pause after a 429. */
class SpotifyApi(private val http: OkHttpClient, private val auth: Auth) {
    private var blockedUntil = 0L

    suspend fun call(method: String, path: String, body: JSONObject? = null): ApiResult {
        val now = SystemClock.elapsedRealtime()
        if (now < blockedUntil) return ApiResult.Err(429, "Spotify rate limit reached. Try again in ${(blockedUntil - now) / 1000 + 1} seconds.")
        val url = (if (path.startsWith("https://")) path else BASE + path).toHttpUrlOrNull()
        if (url == null || url.host != "api.spotify.com" || !url.encodedPath.startsWith("/v1/")) {
            return ApiResult.Err(0, "Rejected an invalid API address.")
        }
        var forced = false
        while (true) {
            val token = auth.accessToken(forced) ?: return ApiResult.Err(401, "Sign in again in Settings.")
            val payload = body?.toString()?.toRequestBody(JSON) ?: if (method == "GET") null else ByteArray(0).toRequestBody(null)
            val req = Request.Builder().url(url).header("Authorization", "Bearer $token").method(method, payload).build()
            val result = try {
                withContext(Dispatchers.IO) {
                    http.newCall(req).execute().use { r ->
                        val text = r.body?.string().orEmpty()
                        val json = runCatching { JSONObject(text) }.getOrDefault(JSONObject())
                        when {
                            r.code == 401 -> ApiResult.Err(401, "Your sign-in has expired. Connect again.")
                            r.code == 429 -> {
                                val seconds = (r.header("Retry-After")?.toIntOrNull() ?: 60).coerceIn(1, 86400)
                                blockedUntil = SystemClock.elapsedRealtime() + seconds * 1000L
                                ApiResult.Err(429, "Too many requests. Wait $seconds seconds.")
                            }
                            r.code == 403 -> ApiResult.Err(403, "Spotify refused this action: check Premium, allowed users and device restrictions.")
                            r.code == 404 -> ApiResult.Err(404, "Not available. Open Spotify on this phone or choose another device.")
                            !r.isSuccessful -> ApiResult.Err(r.code, "Spotify request failed (HTTP ${r.code}). Try again.")
                            else -> ApiResult.Ok(json)
                        }
                    }
                }
            } catch (e: IOException) {
                return ApiResult.Err(0, "Spotify is unreachable. Check your internet connection.")
            }
            if (result is ApiResult.Err && result.code == 401 && !forced) { forced = true; continue }
            return result
        }
    }

    companion object {
        const val BASE = "https://api.spotify.com/v1"
        private val JSON = "application/json".toMediaType()

        fun parseTrack(o: JSONObject): Track? {
            val type = o.optString("type")
            if (o.optBoolean("is_local") || type !in setOf("track", "playlist", "album", "artist")) return null
            val names = o.optJSONArray("artists").names()
            val album = o.optJSONObject("album")
            val subtitle = if (type == "artist") listOf("Artist") else names.ifEmpty { listOfNotNull(o.optJSONObject("owner")?.optString("display_name")?.takeIf { it.isNotEmpty() }) }
            val images = (if (type == "track") album else o)?.optJSONArray("images")
            val image = images?.optJSONObject(0)?.optString("url")
            return Track(
                id = o.optString("id"),
                name = o.optString("name", "Unavailable"),
                subtitle = subtitle.joinToString(", "),
                album = album?.optString("name").orEmpty(),
                uri = o.optString("uri"),
                cover = image?.takeIf { safeImage(it) },
                type = type,
                durationMs = o.optLong("duration_ms"),
                url = o.optJSONObject("external_urls")?.optString("spotify").orEmpty(),
            )
        }

        /** Album art comes from scdn.co; playlist covers also from spotifycdn.com. */
        private fun safeImage(url: String): Boolean {
            val u = url.toHttpUrlOrNull() ?: return false
            return u.scheme == "https" && (u.host.endsWith(".scdn.co") || u.host.endsWith(".spotifycdn.com"))
        }

        private fun JSONArray?.names(): List<String> =
            if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it)?.optString("name") }

        fun parseDevice(o: JSONObject) = Device(
            id = o.optString("id"), name = o.optString("name"), type = o.optString("type"),
            active = o.optBoolean("is_active"), supportsVolume = o.optBoolean("supports_volume"),
            volume = o.optInt("volume_percent"),
        )

        /** Parses GET /me/player; null when nothing is active (204). */
        fun parsePlayback(o: JSONObject): Playback? {
            val item = o.optJSONObject("item") ?: return null
            val track = parseTrack(item) ?: return null
            val device = o.optJSONObject("device") ?: JSONObject()
            val disallows = o.optJSONObject("actions")?.optJSONObject("disallows")
            return Playback(
                track = track,
                playing = o.optBoolean("is_playing"),
                progressMs = o.optLong("progress_ms"),
                deviceId = device.optString("id"),
                deviceName = device.optString("name"),
                volume = device.optInt("volume_percent"),
                supportsVolume = device.optBoolean("supports_volume"),
                restricted = device.optBoolean("is_restricted"),
                shuffle = o.optBoolean("shuffle_state"),
                repeat = o.optString("repeat_state", "off"),
                disallows = disallows?.keys()?.asSequence()?.filter { disallows.optBoolean(it) }?.toSet() ?: emptySet(),
                readAt = SystemClock.elapsedRealtime(),
            )
        }
    }
}
