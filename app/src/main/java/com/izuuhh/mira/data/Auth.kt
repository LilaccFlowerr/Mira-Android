package com.izuuhh.mira.data

import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

/** Spotify Authorization Code flow with PKCE; the browser returns to mira://callback. */
class Auth(
    private val http: OkHttpClient,
    private val prefs: Prefs,
    private val store: TokenStore,
) {
    private var verifier: String? = null
    private var state: String? = null
    private var accessToken: String? = null
    private var expiresAt = 0L
    private var refreshToken: String? = store.read()
    private val lock = Mutex()

    val hasSession get() = refreshToken != null

    /** Builds the Spotify authorize URL, or null when the Client ID is invalid. */
    fun startLogin(): Uri? {
        val id = prefs.clientId
        if (!CLIENT_ID.matches(id)) return null
        verifier = random()
        state = random()
        val challenge = b64(MessageDigest.getInstance("SHA-256").digest(verifier!!.toByteArray()))
        return Uri.parse("https://accounts.spotify.com/authorize").buildUpon()
            .appendQueryParameter("client_id", id)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", REDIRECT)
            .appendQueryParameter("scope", SCOPES)
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .build()
    }

    /** Handles the mira://callback link. Returns an error message, or null on success. */
    suspend fun finishLogin(uri: Uri): String? {
        val expected = state
        val v = verifier
        state = null
        verifier = null
        if (expected == null || v == null || uri.getQueryParameter("state") != expected) return "Invalid sign-in response."
        val code = uri.getQueryParameter("code")
        if (uri.getQueryParameter("error") != null || code.isNullOrEmpty()) return "Access was not granted."
        return token(
            "grant_type" to "authorization_code", "code" to code,
            "redirect_uri" to REDIRECT, "code_verifier" to v,
        )
    }

    /** Returns a valid access token, refreshing it when needed. Null means sign in again. */
    suspend fun accessToken(force: Boolean = false): String? = lock.withLock {
        val cached = accessToken
        if (!force && cached != null && System.currentTimeMillis() < expiresAt) return cached
        val refresh = refreshToken ?: return null
        token("grant_type" to "refresh_token", "refresh_token" to refresh)
        accessToken
    }

    fun logout() {
        accessToken = null
        refreshToken = null
        verifier = null
        state = null
        store.clear()
    }

    private suspend fun token(vararg fields: Pair<String, String>): String? = withContext(Dispatchers.IO) {
        val body = FormBody.Builder().apply {
            fields.forEach { add(it.first, it.second) }
            add("client_id", prefs.clientId)
        }.build()
        try {
            http.newCall(Request.Builder().url("https://accounts.spotify.com/api/token").post(body).build()).execute().use { r ->
                val json = runCatching { JSONObject(r.body?.string().orEmpty()) }.getOrDefault(JSONObject())
                if (json.optString("error") == "invalid_grant") {
                    logout()
                    return@withContext "Access expired or was revoked. Sign in again."
                }
                if (!r.isSuccessful || json.optString("access_token").isEmpty()) {
                    return@withContext if (r.code == 429) "Rate limit reached. Try again later." else "Could not connect to Spotify. Check your Client ID and redirect URI."
                }
                val granted = json.optString("scope").split(' ')
                if (SCOPES.split(' ').any { it !in granted }) {
                    logout()
                    return@withContext "Not all required permissions were granted. Connect again."
                }
                accessToken = json.getString("access_token")
                expiresAt = System.currentTimeMillis() + (json.optLong("expires_in", 3600) - 60).coerceAtLeast(1) * 1000
                json.optString("refresh_token").takeIf { it.isNotEmpty() }?.let {
                    refreshToken = it
                    store.write(it)
                }
                null
            }
        } catch (e: java.io.IOException) {
            "Spotify is unreachable. Check your internet connection."
        }
    }

    private fun random(): String = b64(ByteArray(32).also { SecureRandom().nextBytes(it) })
    private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

    companion object {
        const val REDIRECT = "mira://callback"
        const val SCOPES = "user-read-playback-state user-modify-playback-state user-library-read user-library-modify playlist-read-private"
        private val CLIENT_ID = Regex("^[A-Za-z0-9]{32}$")
    }
}
