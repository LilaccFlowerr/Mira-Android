package com.izuuhh.mira.data

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("mira", Context.MODE_PRIVATE)

    var clientId: String
        get() = sp.getString("clientId", "") ?: ""
        set(v) = sp.edit().putString("clientId", v.trim()).apply()
    var dark: Boolean
        get() = sp.getBoolean("dark", true)
        set(v) = sp.edit().putBoolean("dark", v).apply()
    /** 0 = violet, 1 = green, 2 = peach, same as the desktop app. */
    var accent: Int
        get() = sp.getInt("accent", 0)
        set(v) = sp.edit().putInt("accent", v.coerceIn(0, 2)).apply()
}
