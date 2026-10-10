package com.izuuhh.mira.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.izuuhh.mira.MiraViewModel
import com.izuuhh.mira.Page
import com.izuuhh.mira.ui.components.navTab
import com.izuuhh.mira.SettingsViewModel
import com.izuuhh.mira.ui.theme.AccentPresets
import com.izuuhh.mira.ui.theme.Mira
import com.izuuhh.mira.ui.theme.dynamicColorAvailable

private val ClientIdFormat = Regex("^[0-9a-fA-F]{32}$")

private class Category(val id: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val Categories = listOf(
    Category("spotify", "Spotify", Icons.Rounded.LibraryMusic),
    Category("appearance", "Appearance", Icons.Rounded.Palette),
    Category("style", "Style", Icons.Rounded.Brush),
    Category("navigation", "Navigation", Icons.Rounded.Menu),
    Category("player", "Player and behaviour", Icons.Rounded.PlayCircle),
)

/** A short menu of categories; each one opens its own page, so no page is a long list of everything. */
@Composable
fun SettingsScreen(vm: MiraViewModel) {
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = open != null) { open = null }
    AnimatedContent(
        open, label = "settings-page",
        transitionSpec = {
            val deeper = targetState != null
            (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { if (deeper) it / 6 else -it / 6 }) togetherWith
                (fadeOut(tween(140)) + slideOutHorizontally(tween(260)) { if (deeper) -it / 6 else it / 6 })
        },
    ) { id ->
        if (id == null) SettingsMenu(vm) { open = it } else SettingsPage(vm, Categories.first { it.id == id }) { open = null }
    }
}

@Composable
private fun SettingsMenu(vm: MiraViewModel, onOpen: (String) -> Unit) {
    val settings: SettingsViewModel = viewModel()
    val m = Mira.colors
    val summary = mapOf(
        "spotify" to if (vm.connected) "Connected" else "Not connected",
        "appearance" to listOf("System", "Light", "Dark")[settings.themeMode] + " theme" +
            if (settings.dynamicColor) ", wallpaper colours" else ", " + (AccentPresets.firstOrNull { it.second == settings.hue }?.first ?: "custom") + " accent",
        "style" to listOf("Filled", "Outlined", "Attached")[settings.cardStyle] + " cards, " +
            listOf("sharp", "default", "round")[settings.corners].lowercase() + " corners",
        "navigation" to "${settings.visibleTabs.size} tabs",
        "player" to buildList { if (settings.wavy) add("Wavy progress"); if (vm.lyricsEnabled) add("lyrics"); if (vm.closeSpotify) add("pause on close") }
            .joinToString(", ").ifEmpty { "Everything off" },
    )
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 18.dp)) {
        item { Text("Settings", color = m.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) }
        items(Categories, key = { it.id }) { c ->
            ListItem(
                leadingContent = { Icon(c.icon, null, tint = m.primary) },
                headlineContent = { Text(c.title) },
                supportingContent = { Text(summary[c.id].orEmpty(), fontSize = 12.sp, maxLines = 1) },
                trailingContent = { Icon(Icons.Rounded.ChevronRight, null, tint = m.muted) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent, headlineColor = m.text, supportingColor = m.muted),
                modifier = Modifier.clickable { onOpen(c.id) },
            )
        }
        item { Text("Independent Spotify companion. Not affiliated with Spotify.", color = m.muted, fontSize = 11.sp, modifier = Modifier.padding(18.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsPage(vm: MiraViewModel, category: Category, onBack: () -> Unit) {
    val settings: SettingsViewModel = viewModel()
    val m = Mira.colors
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 18.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = m.text) }
                Text(category.title, color = m.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
        when (category.id) {
            "spotify" -> spotifyItems(vm)
            "appearance" -> appearanceItems(settings)
            "style" -> {
                item { Choice("Cards", listOf("Filled", "Outlined", "Attached"), settings.cardStyle) { settings.updateCardStyle(it) } }
                item { Choice("Corners", listOf("Sharp", "Default", "Round"), settings.corners) { settings.updateCorners(it) } }
                item {
                    Choice(
                        "Font", listOf("Default", "Serif", "Mono"), settings.font,
                        families = listOf(FontFamily.Default, FontFamily.Serif, FontFamily.Monospace),
                    ) { settings.updateFont(it) }
                }
                item { Choice("Text size", listOf("Small", "Default", "Large"), settings.textSize) { settings.updateTextSize(it) } }
            }
            "navigation" -> {
                item { Choice("Labels", listOf("Always", "Active", "Never"), settings.navLabels) { settings.updateNavLabels(it) } }
                item { Choice("Indicator", listOf("Pill", "Circle", "Underline"), settings.navIndicator) { settings.updateNavIndicator(it) } }
                item { Header("Tabs") }
                items(settings.tabOrder, key = { it.name }) { page -> TabRow(page, settings) }
            }
            else -> {
                item { SwitchRow("Wavy progress", "The progress line waves while music plays", settings.wavy) { settings.updateWavy(it) } }
                item { SwitchRow("Blurred background", "Album art blurred behind the player (Android 12+)", settings.blurBackground) { settings.updateBlurBackground(it) } }
                item { SwitchRow("Lyrics", "Fetched from lrclib.net, which receives the song and artist name", vm.lyricsEnabled) { vm.updateLyrics(it) } }
                item { SwitchRow("Pause Spotify when Mira closes", "Pauses the music and disconnects when you swipe Mira away", vm.closeSpotify) { vm.updateCloseSpotify(it) } }
                item { SwitchRow("Genre shortcuts", "Show the genre cards on Home", settings.showGenres) { settings.updateShowGenres(it) } }
            }
        }
    }
}

private fun LazyListScope.spotifyItems(vm: MiraViewModel) = item {
    val m = Mira.colors
    var clientId by remember { mutableStateOf(vm.prefs.clientId) }
    var showHelp by remember { mutableStateOf(!vm.connected) }
    Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (showHelp) {
            Text(
                "Create an app at developer.spotify.com, add the redirect URI mira://callback and paste its Client ID here. Playback needs Spotify Premium and the Spotify app on this phone.",
                color = m.muted, fontSize = 13.sp,
            )
        } else {
            TextButton(onClick = { showHelp = true }, contentPadding = PaddingValues(0.dp)) { Text("How do I set this up?", color = m.primary, fontSize = 13.sp) }
        }
        val valid = ClientIdFormat.matches(clientId)
        OutlinedTextField(
            clientId, { clientId = it.trim(); vm.setClientId(clientId) }, label = { Text("Client ID") }, singleLine = true,
            enabled = !vm.connected, modifier = Modifier.fillMaxWidth(),
            isError = clientId.isNotEmpty() && !valid,
            supportingText = { if (clientId.isNotEmpty() && !valid) Text("A Client ID is 32 letters and digits") },
            trailingIcon = { if (valid) Icon(Icons.Rounded.CheckCircle, "Looks valid", tint = m.primary) },
        )
        if (vm.authMessage.isNotEmpty()) Text(vm.authMessage, color = m.muted, fontSize = 13.sp)
        if (vm.connected) OutlinedButton(onClick = { vm.logout() }) { Text("Disconnect") }
        else Button(
            onClick = { vm.login() }, enabled = !vm.signingIn,
            colors = ButtonDefaults.buttonColors(containerColor = m.primary, contentColor = m.onPrimary),
        ) { Text(if (vm.signingIn) "Waiting for Spotify…" else "Connect Spotify") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.appearanceItems(settings: SettingsViewModel) {
    item { Choice("Theme", listOf("System", "Light", "Dark"), settings.themeMode) { settings.updateThemeMode(it) } }
    if (dynamicColorAvailable) item { SwitchRow("Wallpaper colours", "Material You: colours follow your wallpaper", settings.dynamicColor) { settings.updateDynamic(it) } }
    item { SwitchRow("True black", "Pure black backgrounds in dark mode, easier on AMOLED screens", settings.amoled) { settings.updateAmoled(it) } }
    if (!settings.dynamicColor) item {
        val m = Mira.colors
        Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Accent colour", color = m.text)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AccentPresets.forEach { (name, hue) ->
                    val selected = settings.hue == hue
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(Color.hsl(hue.toFloat(), 0.6f, 0.62f))
                            .border(if (selected) 3.dp else 0.dp, m.text, CircleShape)
                            .clickable { settings.updateHue(hue) },
                        contentAlignment = Alignment.Center,
                    ) { if (selected) Icon(Icons.Rounded.Check, name, tint = Color.Black.copy(alpha = 0.7f)) }
                }
            }
            Slider(
                value = settings.hue.toFloat(), onValueChange = { settings.updateHue(it.toInt()) }, valueRange = 0f..359f,
                colors = SliderDefaults.colors(thumbColor = m.primary, activeTrackColor = m.primary, inactiveTrackColor = m.outline),
            )
        }
    }
}

@Composable
private fun TabRow(page: Page, settings: SettingsViewModel) {
    val m = Mira.colors
    val tab = navTab(page)
    val i = settings.tabOrder.indexOf(page)
    val shown = page !in settings.hiddenTabs
    ListItem(
        leadingContent = { Icon(tab.icon, null, tint = if (shown) m.primary else m.muted) },
        headlineContent = { Text(tab.label) },
        supportingContent = if (page == Page.Settings) ({ Text("Always shown", fontSize = 12.sp) }) else null,
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton({ settings.moveTab(page, -1) }, enabled = i > 0) { Icon(Icons.Rounded.KeyboardArrowUp, "Move up") }
                IconButton({ settings.moveTab(page, 1) }, enabled = i < settings.tabOrder.lastIndex) { Icon(Icons.Rounded.KeyboardArrowDown, "Move down") }
                Switch(
                    shown, { settings.setTabVisible(page, it) }, enabled = page != Page.Settings,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = m.onPrimary, checkedTrackColor = m.primary,
                        uncheckedThumbColor = m.muted, uncheckedTrackColor = m.elevated, uncheckedBorderColor = m.muted,
                    ),
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent, headlineColor = m.text, supportingColor = m.muted),
    )
}

@Composable
private fun Header(text: String) {
    Text(
        text, color = Mira.colors.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 6.dp),
    )
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val m = Mira.colors
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, fontSize = 12.sp) },
        trailingContent = {
            Switch(
                checked, onChange,
                // The theme's outline colour is nearly invisible on dark surfaces, so the off state is drawn with the muted text colour.
                thumbContent = if (checked) ({ Icon(Icons.Rounded.Check, null, Modifier.size(SwitchDefaults.IconSize)) }) else null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = m.onPrimary, checkedTrackColor = m.primary, checkedIconColor = m.primary,
                    uncheckedThumbColor = m.muted, uncheckedTrackColor = m.elevated, uncheckedBorderColor = m.muted,
                ),
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent, headlineColor = m.text, supportingColor = m.muted),
        modifier = Modifier.clickable { onChange(!checked) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Choice(title: String, options: List<String>, selected: Int, families: List<FontFamily>? = null, onSelect: (Int) -> Unit) {
    val m = Mira.colors
    Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = m.text)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, name ->
                SegmentedButton(
                    selected = selected == i, onClick = { onSelect(i) },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = m.container, activeContentColor = m.primary, activeBorderColor = m.primary,
                        inactiveContainerColor = Color.Transparent, inactiveContentColor = m.text, inactiveBorderColor = m.muted.copy(alpha = 0.6f),
                    ),
                ) { Text(name, fontFamily = families?.get(i)) }
            }
        }
    }
}
