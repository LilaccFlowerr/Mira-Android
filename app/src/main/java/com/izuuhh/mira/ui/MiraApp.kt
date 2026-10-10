package com.izuuhh.mira.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.izuuhh.mira.Event
import com.izuuhh.mira.MiraViewModel
import com.izuuhh.mira.Page
import com.izuuhh.mira.ui.components.NavBar
import com.izuuhh.mira.ui.components.SearchBar
import com.izuuhh.mira.ui.player.DevicesSheet
import com.izuuhh.mira.ui.player.NowPlaying
import com.izuuhh.mira.ui.player.PlayerBar
import com.izuuhh.mira.ui.screens.MusicScreen
import com.izuuhh.mira.ui.screens.SettingsScreen
import com.izuuhh.mira.ui.theme.Mira

@Composable
fun MiraApp(vm: MiraViewModel, onEvent: (Event) -> Unit) {
    val m = Mira.colors
    val snackbar = remember { SnackbarHostState() }
    var showPlayer by remember { mutableStateOf(false) }
    var showDevices by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.eventFlow.collect { e ->
            if (e is Event.Message) snackbar.showSnackbar(e.text) else onEvent(e)
        }
    }
    BackHandler(enabled = showPlayer) { showPlayer = false }
    BackHandler(enabled = !showPlayer && vm.page != Page.Home) { vm.back() }

    Box(Modifier.fillMaxSize().background(m.base)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).padding(horizontal = 8.dp)) {
            SearchBar(vm)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(m.panel)) {
                when (vm.page) {
                    Page.Settings -> SettingsScreen(vm)
                    else -> MusicScreen(vm, openPlayer = { showPlayer = true }, goSettings = { vm.navigate(Page.Settings) })
                }
            }
            Spacer(Modifier.height(8.dp))
            PlayerBar(vm, onExpand = { showPlayer = true }, onDevices = { showDevices = true })
            Spacer(Modifier.height(8.dp))
            NavBar(vm)
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 190.dp))
        if (showPlayer) NowPlaying(vm, onClose = { showPlayer = false }, onDevices = { showDevices = true })
        if (showDevices) DevicesSheet(vm, onClose = { showDevices = false })
    }
}
