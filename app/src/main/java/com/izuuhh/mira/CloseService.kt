package com.izuuhh.mira

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.izuuhh.mira.data.Prefs

/** Lets the running App Remote connection be reached after the UI is gone. Set by [MiraViewModel]. */
object CloseHook {
    @Volatile var pause: (() -> Unit)? = null
    @Volatile var disconnect: (() -> Unit)? = null
}

/**
 * Started with the app and kept alive after the task is swiped away. When "Pause Spotify when Mira closes"
 * is on, it pauses and drops the App Remote connection. Spotify's own foreground service is not ours to stop.
 */
class CloseService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (Prefs(this).closeSpotify) {
            CloseHook.pause?.invoke()
            // Give the pause a moment to reach Spotify before cutting the connection.
            Handler(Looper.getMainLooper()).postDelayed({
                CloseHook.disconnect?.invoke()
                stopSelf()
            }, 800)
        } else {
            stopSelf()
        }
    }
}
