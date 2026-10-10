package com.izuuhh.mira

import android.net.Uri

enum class Page { Home, Search, Liked, Playlists, Playlist, Album, Settings }

/** One-off things the UI has to do: open a URL, show a snackbar, wake the Spotify app. */
sealed interface Event {
    data class OpenBrowser(val uri: Uri) : Event
    data class Message(val text: String) : Event
    data object OpenSpotifyApp : Event
    data class OpenUrl(val url: String) : Event
    /** Spotify has to show its "allow Mira" screen; the Activity runs it and reports back. */
    data class RequestConsent(val clientId: String) : Event
}
