package com.izuuhh.mira

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import com.izuuhh.mira.ui.MiraApp
import com.izuuhh.mira.ui.theme.MiraTheme

class MainActivity : ComponentActivity() {
    private val vm: MiraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        handle(intent)
        setContent {
            MiraTheme(dark = vm.dark, accent = vm.accent) {
                MiraApp(vm, onEvent = ::onEvent)
            }
        }
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
            is Event.Message -> {}
        }
    }
}
