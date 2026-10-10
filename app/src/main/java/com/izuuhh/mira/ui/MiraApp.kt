package com.izuuhh.mira.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import com.izuuhh.mira.ui.theme.corner
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
            AnimatedVisibility(!vm.online, enter = fadeIn(tween(200)) + expandVertically(tween(250)), exit = fadeOut(tween(150)) + shrinkVertically(tween(200))) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(corner(16.dp))).background(m.elevated).padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.WifiOff, null, tint = m.error)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("No internet connection", color = m.text, fontWeight = FontWeight.Bold)
                        Text("Mira reconnects by itself as soon as you are back online.", color = m.muted, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(corner(20.dp))).background(m.panel)) {
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
        AnimatedVisibility(
            showPlayer,
            enter = slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(300, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(200)),
        ) { NowPlaying(vm, onClose = { showPlayer = false }, onDevices = { showDevices = true }) }
        if (showDevices) DevicesSheet(vm, onClose = { showDevices = false })
    }
}
