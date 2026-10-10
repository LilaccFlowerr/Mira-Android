package com.izuuhh.mira

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.izuuhh.mira.data.Prefs

/** Appearance preferences: each one is kept in state for Compose and written through to [Prefs]. */
class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)

    var themeMode by mutableIntStateOf(prefs.themeMode); private set
    var hue by mutableIntStateOf(prefs.hue); private set
    var dynamicColor by mutableStateOf(prefs.dynamicColor); private set
    var amoled by mutableStateOf(prefs.amoled); private set
    var corners by mutableIntStateOf(prefs.corners); private set
    var font by mutableIntStateOf(prefs.font); private set
    var textSize by mutableIntStateOf(prefs.textSize); private set
    var wavy by mutableStateOf(prefs.wavy); private set
    var blurBackground by mutableStateOf(prefs.blurBackground); private set
    var showGenres by mutableStateOf(prefs.showGenres); private set

    fun updateThemeMode(v: Int) { themeMode = v; prefs.themeMode = v }
    fun updateHue(v: Int) { hue = v; prefs.hue = v; if (dynamicColor) updateDynamic(false) }
    fun updateDynamic(v: Boolean) { dynamicColor = v; prefs.dynamicColor = v }
    fun updateAmoled(v: Boolean) { amoled = v; prefs.amoled = v }
    fun updateCorners(v: Int) { corners = v; prefs.corners = v }
    fun updateFont(v: Int) { font = v; prefs.font = v }
    fun updateTextSize(v: Int) { textSize = v; prefs.textSize = v }
    fun updateWavy(v: Boolean) { wavy = v; prefs.wavy = v }
    fun updateBlurBackground(v: Boolean) { blurBackground = v; prefs.blurBackground = v }
    fun updateShowGenres(v: Boolean) { showGenres = v; prefs.showGenres = v }
}
