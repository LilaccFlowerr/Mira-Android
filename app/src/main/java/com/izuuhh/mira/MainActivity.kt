package com.izuuhh.mira

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import com.izuuhh.mira.data.Auth
import com.izuuhh.mira.ui.MiraApp
import com.izuuhh.mira.ui.theme.MiraTheme
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse

class MainActivity : ComponentActivity() {
    private val vm: MiraViewModel by viewModels()

    private val consent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val response = AuthorizationClient.getResponse(result.resultCode, result.data)
        vm.onConsentResult(response.type == AuthorizationResponse.Type.TOKEN)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        handle(intent)
        vm.host = this
        setContent {
            MiraTheme(dark = vm.dark, accent = vm.accent) {
                MiraApp(vm, onEvent = ::onEvent)
            }
        }
    }

    override fun onDestroy() {
        if (vm.host === this) vm.host = null
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "mira" && data.host == "callback") vm.onCallback(data)
    }

    private fun onEvent(e: Event) {
        when (e) {
            is Event.OpenBrowser -> CustomTabsIntent.Builder().build().launchUrl(this, e.uri)
            Event.OpenSpotifyApp -> {
                val launch = packageManager.getLaunchIntentForPackage("com.spotify.music")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("spotify:"))
                runCatching { startActivity(launch) }
            }
            is Event.RequestConsent -> {
                val request = AuthorizationRequest.Builder(e.clientId, AuthorizationResponse.Type.TOKEN, Auth.REDIRECT)
                    .setScopes(arrayOf("app-remote-control"))
                    .build()
                consent.launch(AuthorizationClient.createLoginActivityIntent(this, request))
            }
            is Event.Message -> {}
        }
    }
}
