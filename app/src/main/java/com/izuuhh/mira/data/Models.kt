package com.izuuhh.mira.data

data class Track(
    val id: String,
    val name: String,
    val subtitle: String,
    val album: String,
    val uri: String,
    val cover: String?,
    val type: String,
    val durationMs: Long,
    val url: String,
)

data class Device(
    val id: String,
    val name: String,
    val type: String,
    val active: Boolean,
    val supportsVolume: Boolean,
    val volume: Int,
)

data class Playback(
    val track: Track,
    val playing: Boolean,
    val progressMs: Long,
    val deviceId: String,
    val deviceName: String,
    val volume: Int,
    val supportsVolume: Boolean,
    val restricted: Boolean,
    val shuffle: Boolean,
    val repeat: String,
    val disallows: Set<String>,
    /** SystemClock.elapsedRealtime() when progressMs was read; used to interpolate the position. */
    val readAt: Long,
) {
    fun position(now: Long): Long {
        val ms = if (playing) progressMs + (now - readAt) else progressMs
        return if (track.durationMs > 0) minOf(ms, track.durationMs) else ms
    }

    fun allows(action: String) = !restricted && action !in disallows
}

enum class Collection { None, Search, Library, Playlists, Playlist }
