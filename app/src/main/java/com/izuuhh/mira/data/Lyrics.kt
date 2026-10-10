package com.izuuhh.mira.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

data class LyricLine(val timeMs: Long, val text: String)

/** Lyrics for one song: [synced] has a time per line, [plain] is just text. Either can be empty. */
data class Lyrics(val synced: List<LyricLine>, val plain: String)

sealed interface LyricsState {
    data object Idle : LyricsState
    data object Loading : LyricsState
    data object NotFound : LyricsState
    data object Failed : LyricsState
    data class Found(val lyrics: Lyrics) : LyricsState
}

/** Looks lyrics up on lrclib.net, a free and open lyrics service. Only the song and artist name are sent. */
class LyricsClient(private val http: OkHttpClient) {
    suspend fun fetch(title: String, artist: String, album: String, durationMs: Long): LyricsState = withContext(Dispatchers.IO) {
        try {
            val exact = "https://lrclib.net/api/get".toHttpUrl().newBuilder()
                .addQueryParameter("track_name", title)
                .addQueryParameter("artist_name", artist)
                .apply {
                    if (album.isNotBlank()) addQueryParameter("album_name", album)
                    if (durationMs > 0) addQueryParameter("duration", (durationMs / 1000).toString())
                }.build()
            get(exact)?.let { parse(JSONObject(it)) }?.let { return@withContext LyricsState.Found(it) }
            // No exact match: take the best search result instead.
            val search = "https://lrclib.net/api/search".toHttpUrl().newBuilder()
                .addQueryParameter("track_name", title).addQueryParameter("artist_name", artist).build()
            val body = get(search) ?: return@withContext LyricsState.NotFound
            val results = JSONArray(body)
            for (i in 0 until results.length()) {
                parse(results.getJSONObject(i))?.let { return@withContext LyricsState.Found(it) }
            }
            LyricsState.NotFound
        } catch (e: IOException) {
            LyricsState.Failed
        } catch (e: org.json.JSONException) {
            LyricsState.Failed
        }
    }

    private fun get(url: okhttp3.HttpUrl): String? {
        val req = Request.Builder().url(url).header("User-Agent", "Mira Android (github.com/LilaccFlowerr/Mira-Android)").build()
        http.newCall(req).execute().use { r -> return if (r.isSuccessful) r.body?.string() else null }
    }

    private fun parse(o: JSONObject): Lyrics? {
        val synced = parseLrc(o.optString("syncedLyrics").takeIf { it != "null" }.orEmpty())
        val plain = o.optString("plainLyrics").takeIf { it != "null" }.orEmpty()
        if (synced.isEmpty() && plain.isBlank()) return null
        return Lyrics(synced, plain)
    }

    companion object {
        private val TIME = Regex("\\[(\\d+):(\\d+)(?:[.:](\\d+))?]")

        /** Parses "[01:23.45] words" lines; a line can carry several timestamps. */
        fun parseLrc(text: String): List<LyricLine> {
            val lines = ArrayList<LyricLine>()
            for (raw in text.lineSequence()) {
                val stamps = TIME.findAll(raw).toList()
                if (stamps.isEmpty()) continue
                val words = raw.substring(stamps.last().range.last + 1).trim()
                for (m in stamps) {
                    val min = m.groupValues[1].toLong()
                    val sec = m.groupValues[2].toLong()
                    val frac = m.groupValues[3]
                    val ms = if (frac.isEmpty()) 0L else (frac + "00").take(3).toLong()
                    lines.add(LyricLine((min * 60 + sec) * 1000 + ms, words))
                }
            }
            return lines.sortedBy { it.timeMs }
        }
    }
}
