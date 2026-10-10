package com.izuuhh.mira.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.izuuhh.mira.MiraViewModel
import com.izuuhh.mira.ui.theme.AccentPresets
import com.izuuhh.mira.ui.theme.Mira
import com.izuuhh.mira.ui.theme.dynamicColorAvailable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

@Composable
fun SettingsScreen(vm: MiraViewModel) {
    val m = Mira.colors
    var clientId by remember { mutableStateOf(vm.prefs.clientId) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Settings", color = m.text, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Spotify", color = m.primary, fontWeight = FontWeight.Bold)
                Text(
                    "Create an app at developer.spotify.com, add the redirect URI mira://callback and paste its Client ID here. Playback needs Spotify Premium and the Spotify app on this phone.",
                    color = m.muted, fontSize = 13.sp,
                )
                OutlinedTextField(
                    clientId, { clientId = it.trim(); vm.setClientId(clientId) }, label = { Text("Client ID") }, singleLine = true,
                    enabled = !vm.connected, modifier = Modifier.fillMaxWidth(),
                )
                if (vm.authMessage.isNotEmpty()) Text(vm.authMessage, color = m.muted, fontSize = 13.sp)
                if (vm.connected) OutlinedButton(onClick = { vm.logout() }) { Text("Disconnect") }
                else Button(onClick = { vm.login() }, enabled = !vm.signingIn, colors = ButtonDefaults.buttonColors(containerColor = m.primary, contentColor = m.onPrimary)) {
                    Text(if (vm.signingIn) "Waiting for Spotify…" else "Connect Spotify")
                }
            }
        }
        item { AppearanceSection(vm) }
        item { StyleSection(vm) }
        item { Text("Independent Spotify companion. Not affiliated with Spotify.", color = m.muted, fontSize = 11.sp) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceSection(vm: MiraViewModel) {
    val m = Mira.colors
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Appearance", color = m.primary, fontWeight = FontWeight.Bold)

        Text("Theme", color = m.text)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("System", "Light", "Dark").forEachIndexed { i, name ->
                FilterChip(vm.themeMode == i, { vm.updateThemeMode(i) }, label = { Text(name) })
            }
        }

        if (dynamicColorAvailable) SwitchRow("Use wallpaper colours", "Material You: colours follow your wallpaper", vm.dynamicColor) { vm.updateDynamic(it) }
        SwitchRow("True black", "Pure black backgrounds in dark mode, easier on AMOLED screens", vm.amoled) { vm.updateAmoled(it) }

        if (!vm.dynamicColor) {
            Text("Accent colour", color = m.text)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AccentPresets.forEach { (name, hue) ->
                    val selected = vm.hue == hue
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(Color.hsl(hue.toFloat(), 0.6f, 0.62f))
                            .border(if (selected) 3.dp else 0.dp, m.text, CircleShape)
                            .clickable { vm.updateHue(hue) },
                        contentAlignment = Alignment.Center,
                    ) { if (selected) Icon(Icons.Rounded.Check, name, tint = Color.Black.copy(alpha = 0.7f)) }
                }
            }
            Text("Custom hue", color = m.muted, fontSize = 13.sp)
            Slider(
                value = vm.hue.toFloat(), onValueChange = { vm.updateHue(it.toInt()) }, valueRange = 0f..359f,
                colors = SliderDefaults.colors(thumbColor = m.primary, activeTrackColor = m.primary, inactiveTrackColor = m.outline),
            )
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val m = Mira.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = m.text)
            Text(subtitle, color = m.muted, fontSize = 12.sp)
        }
        Switch(checked, onChange)
    }
}

@Composable
private fun StyleSection(vm: MiraViewModel) {
    val m = Mira.colors
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Style", color = m.primary, fontWeight = FontWeight.Bold)

        OptionRow("Corners", listOf("Sharp", "Default", "Round"), vm.corners) { vm.updateCorners(it) }
        OptionRow("Font", listOf("Default", "Serif", "Mono"), vm.font) { vm.updateFont(it) }
        OptionRow("Text size", listOf("Small", "Default", "Large"), vm.textSize) { vm.updateTextSize(it) }

        SwitchRow("Wavy progress", "The progress line waves while music plays", vm.wavy) { vm.updateWavy(it) }
        SwitchRow("Blurred player background", "Album art blurred behind the player (Android 12+)", vm.blurBackground) { vm.updateBlurBackground(it) }
        SwitchRow("Genre shortcuts", "Show the genre cards on Home", vm.showGenres) { vm.updateShowGenres(it) }
    }
}

@Composable
private fun OptionRow(title: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val m = Mira.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = m.text)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { i, name -> FilterChip(selected == i, { onSelect(i) }, label = { Text(name) }) }
        }
    }
}
