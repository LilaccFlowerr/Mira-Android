package com.izuuhh.mira.data

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("mira", Context.MODE_PRIVATE)

    var clientId: String
        get() = sp.getString("clientId", "") ?: ""
        set(v) = sp.edit().putString("clientId", v.trim()).apply()
    /** 0 = follow the system, 1 = light, 2 = dark. */
    var themeMode: Int
        get() = sp.getInt("themeMode", 2)
        set(v) = sp.edit().putInt("themeMode", v.coerceIn(0, 2)).apply()
    /** Accent colour as a hue in degrees; 262 is the violet from the desktop app. */
    var hue: Int
        get() = sp.getInt("hue", 262)
        set(v) = sp.edit().putInt("hue", v.coerceIn(0, 359)).apply()
    /** Android 12+: take the colours from the wallpaper. */
    var dynamicColor: Boolean
        get() = sp.getBoolean("dynamicColor", false)
        set(v) = sp.edit().putBoolean("dynamicColor", v).apply()
    /** True black backgrounds in dark mode. */
    var amoled: Boolean
        get() = sp.getBoolean("amoled", false)
        set(v) = sp.edit().putBoolean("amoled", v).apply()
    /** 0 = sharp, 1 = default, 2 = extra round. */
    var corners: Int
        get() = sp.getInt("corners", 1)
        set(v) = sp.edit().putInt("corners", v.coerceIn(0, 2)).apply()
    /** 0 = default, 1 = serif, 2 = monospace. */
    var font: Int
        get() = sp.getInt("font", 0)
        set(v) = sp.edit().putInt("font", v.coerceIn(0, 2)).apply()
    /** 0 = small, 1 = default, 2 = large. */
    var textSize: Int
        get() = sp.getInt("textSize", 1)
        set(v) = sp.edit().putInt("textSize", v.coerceIn(0, 2)).apply()
    var wavy: Boolean
        get() = sp.getBoolean("wavy", true)
        set(v) = sp.edit().putBoolean("wavy", v).apply()
    var blurBackground: Boolean
        get() = sp.getBoolean("blurBackground", true)
        set(v) = sp.edit().putBoolean("blurBackground", v).apply()
    var showGenres: Boolean
        get() = sp.getBoolean("showGenres", true)
        set(v) = sp.edit().putBoolean("showGenres", v).apply()
    var recentSearches: List<String>
        get() = (sp.getString("recentSearches", "") ?: "").split('\n').filter { it.isNotBlank() }
        set(v) = sp.edit().putString("recentSearches", v.take(8).joinToString("\n")).apply()
    var lyrics: Boolean
        get() = sp.getBoolean("lyrics", true)
        set(v) = sp.edit().putBoolean("lyrics", v).apply()
}
