package com.izuuhh.mira.data

import android.content.Context
import android.util.Log
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.PlayerApi
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.android.appremote.api.error.CouldNotFindSpotifyApp
import com.spotify.android.appremote.api.error.NotLoggedInException
import com.spotify.android.appremote.api.error.UserNotAuthorizedException
import com.spotify.protocol.client.Subscription
import com.spotify.protocol.types.PlayerState

/**
 * Spotify App Remote: talks to the Spotify app in the background, so Spotify never has to be opened
 * first. It is used for playback commands; browsing and metadata stay on the Web API.
 */
class RemotePlayer(private val context: Context, private val onChange: (PlayerState) -> Unit) {
    private var remote: SpotifyAppRemote? = null
    private var subscription: Subscription<PlayerState>? = null
    private class Waiter(val showAuth: Boolean, val done: (Throwable?) -> Unit)

    private var waiting = mutableListOf<Waiter>()
    private var connecting = false
    private var attemptAuth = false
    private lateinit var lastHost: Context
    private var lastClientId = ""

    /** The player API while connected, otherwise null. */
    val player: PlayerApi? get() = remote?.takeIf { it.isConnected }?.playerApi
    val installed get() = SpotifyAppRemote.isSpotifyInstalled(context)

    /**
     * Connects (waking Spotify in the background). [done] gets null on success or the error.
     * [showAuth] lets Spotify show its consent screen; that only works from a user action.
     */
    fun connect(host: Context, clientId: String, showAuth: Boolean, done: (Throwable?) -> Unit) {
        if (player != null) { done(null); return }
        waiting.add(Waiter(showAuth, done))
        if (connecting) return
        lastHost = host
        lastClientId = clientId
        start(showAuth)
    }

    private fun start(showAuth: Boolean) {
        connecting = true
        attemptAuth = showAuth
        val params = ConnectionParams.Builder(lastClientId)
            .setRedirectUri(Auth.REDIRECT)
            .showAuthView(showAuth)
            .build()
        SpotifyAppRemote.connect(lastHost, params, object : Connector.ConnectionListener {
            override fun onConnected(r: SpotifyAppRemote) {
                Log.i(TAG, "App Remote connected")
                remote = r
                subscription?.cancel()
                subscription = r.playerApi.subscribeToPlayerState().setEventCallback { onChange(it) }
                finish(null)
            }

            override fun onFailure(error: Throwable) {
                Log.w(TAG, "App Remote failed: ${error.javaClass.simpleName}: ${error.message}")
                remote = null
                finish(error)
            }
        })
    }

    private fun finish(error: Throwable?) {
        connecting = false
        val all = waiting
        waiting = mutableListOf()
        if (error != null && needsConsent(error) && !attemptAuth && all.any { it.showAuth }) {
            // A silent attempt was refused; someone tapped meanwhile, so retry with the consent screen.
            all.filter { !it.showAuth }.forEach { it.done(error) }
            waiting.addAll(all.filter { it.showAuth })
            start(true)
        } else {
            all.forEach { it.done(error) }
        }
    }

    companion object {
        private const val TAG = "MiraRemote"

        fun needsConsent(e: Throwable) = e is UserNotAuthorizedException

        fun describe(e: Throwable) = when (e) {
            is CouldNotFindSpotifyApp -> "The Spotify app is not installed on this phone."
            is NotLoggedInException -> "Log in to the Spotify app first."
            is UserNotAuthorizedException -> "Allow Mira in the Spotify permission screen."
            else -> e.message ?: "Could not connect to the Spotify app."
        }
    }

    fun disconnect() {
        subscription?.cancel()
        subscription = null
        remote?.let { SpotifyAppRemote.disconnect(it) }
        remote = null
    }
}
