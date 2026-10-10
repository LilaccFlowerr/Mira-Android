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

    var cardStyle by mutableIntStateOf(prefs.cardStyle); private set
    var navLabels by mutableIntStateOf(prefs.navLabels); private set
    var navIndicator by mutableIntStateOf(prefs.navIndicator); private set
    var tabOrder by mutableStateOf(readTabs(prefs.tabOrder, complete = true)); private set
    var hiddenTabs by mutableStateOf(readTabs(prefs.tabsHidden, complete = false).toSet()); private set

    /** The tabs shown in the bar, in order. Settings always stays so the options can never be locked away. */
    val visibleTabs get() = tabOrder.filter { it !in hiddenTabs || it == Page.Settings }

    fun updateCardStyle(v: Int) { cardStyle = v; prefs.cardStyle = v }
    fun updateNavLabels(v: Int) { navLabels = v; prefs.navLabels = v }
    fun updateNavIndicator(v: Int) { navIndicator = v; prefs.navIndicator = v }

    fun moveTab(page: Page, by: Int) {
        val i = tabOrder.indexOf(page)
        val j = i + by
        if (i < 0 || j !in tabOrder.indices) return
        tabOrder = tabOrder.toMutableList().also { it[i] = it[j]; it[j] = page }
        prefs.tabOrder = tabOrder.joinToString(",")
    }

    fun setTabVisible(page: Page, visible: Boolean) {
        if (page == Page.Settings) return
        hiddenTabs = if (visible) hiddenTabs - page else hiddenTabs + page
        prefs.tabsHidden = hiddenTabs.joinToString(",")
    }

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

    private companion object {
        val ALL_TABS = listOf(Page.Home, Page.Search, Page.Liked, Page.Playlists, Page.Settings)

        /** Reads saved page names; with [complete], tabs missing from the list are appended so none can get lost. */
        fun readTabs(saved: String, complete: Boolean): List<Page> {
            val read = saved.split(',').mapNotNull { name -> runCatching { Page.valueOf(name) }.getOrNull() }.filter { it in ALL_TABS }.distinct()
            return if (complete) read + ALL_TABS.filter { it !in read } else read
        }
    }
}
