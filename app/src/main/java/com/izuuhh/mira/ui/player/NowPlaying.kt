package com.izuuhh.mira.ui.player

import androidx.lifecycle.viewmodel.compose.viewModel
import com.izuuhh.mira.SettingsViewModel
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.izuuhh.mira.MiraViewModel
import com.izuuhh.mira.ui.components.Cover
import com.izuuhh.mira.ui.components.RoundButton
import com.izuuhh.mira.ui.components.WavyProgress
import com.izuuhh.mira.ui.components.formatTime
import com.izuuhh.mira.ui.components.rememberPlaybackClock
import com.izuuhh.mira.ui.theme.Mira

@Composable
fun NowPlaying(vm: MiraViewModel, onClose: () -> Unit, onDevices: () -> Unit) {
    val settings: SettingsViewModel = viewModel()
    val m = Mira.colors
    val pb = vm.playback
    val clock = rememberPlaybackClock(pb?.playing == true)
    val seconds by remember { derivedStateOf { (pb?.position(clock.value) ?: 0L) / 1000 } }
    // The cover breathes when you pause or play.
    val coverScale by animateFloatAsState(if (pb?.playing == false) 0.88f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "cover")

    Box(Modifier.fillMaxSize().background(m.base)) {
    // Blurred album art behind everything; a scrim keeps the text readable in both themes.
    val cover = pb?.track?.cover
    if (settings.blurBackground && cover != null && android.os.Build.VERSION.SDK_INT >= 31) {
        AsyncImage(
            cover, null, Modifier.fillMaxSize().blur(70.dp, BlurredEdgeTreatment.Unbounded).graphicsLayer { scaleX = 1.4f; scaleY = 1.4f },
            contentScale = ContentScale.Crop,
        )
        Box(Modifier.fillMaxSize().background(m.base.copy(alpha = 0.66f)))
    }
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).padding(20.dp).clickable(enabled = false) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        var showLyrics by rememberSaveable { mutableStateOf(false) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RoundButton(Icons.Rounded.KeyboardArrowDown, "Close", onClose, selected = true)
            Spacer(Modifier.weight(1f))
            if (vm.lyricsEnabled) RoundButton(Icons.Rounded.Lyrics, "Lyrics", { showLyrics = !showLyrics }, selected = showLyrics, enabled = pb != null)
        }
        val track = pb?.track
        LaunchedEffect(showLyrics, track?.uri) { if (showLyrics && track != null) vm.loadLyrics(track) }
        if (showLyrics && vm.lyricsEnabled) {
            LyricsPanel(vm, positionMs = { pb?.position(clock.value) ?: 0 }, modifier = Modifier.weight(1f).fillMaxWidth())
        } else {
            Spacer(Modifier.weight(1f))
            AnimatedContent(
                targetState = pb?.track?.cover, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }, label = "cover-art",
                modifier = Modifier.graphicsLayer { scaleX = coverScale; scaleY = coverScale },
            ) { cover -> Cover(cover, 300.dp, radius = 32.dp) }
        }
        Spacer(Modifier.height(24.dp))
        Text(pb?.track?.name ?: "Nothing playing", color = m.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Text(pb?.track?.subtitle ?: "Choose a song to start", color = m.muted, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        pb?.track?.album?.takeIf { it.isNotEmpty() }?.let { Text(it, color = m.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        Spacer(Modifier.height(16.dp))
        WavyProgress(
            position = { pb?.position(clock.value) ?: 0 }, duration = pb?.track?.durationMs ?: 0, playing = pb?.playing == true,
            seekable = pb?.allows("seeking") == true, onSeek = { vm.seek(it) }, height = 28.dp,
        )
        Row(Modifier.fillMaxWidth()) {
            Text(formatTime(seconds * 1000), color = m.muted, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            Text(formatTime(pb?.track?.durationMs ?: 0), color = m.muted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundButton(Icons.Rounded.Shuffle, "Shuffle", { vm.setShuffle(pb?.shuffle != true) }, selected = pb?.shuffle == true, enabled = pb != null && pb.allows("toggling_shuffle"))
            RoundButton(Icons.Rounded.SkipPrevious, "Previous", { vm.command("previous") }, enabled = pb != null && pb.allows("skipping_prev"))
            RoundButton(if (pb?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play or pause", { vm.togglePlayback() }, filled = true, size = 68.dp, enabled = pb?.restricted != true)
            RoundButton(Icons.Rounded.SkipNext, "Next", { vm.command("next") }, enabled = pb != null && pb.allows("skipping_next"))
            RoundButton(if (pb?.repeat == "track") Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repeat", { vm.cycleRepeat() }, selected = pb != null && pb.repeat != "off", enabled = pb != null)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = m.muted)
            // Spotify cannot be volume-controlled remotely on this phone, so the slider moves the phone's media volume.
            val useSystem = pb?.supportsVolume != true
            LaunchedEffect(Unit) { vm.refreshSystemVolume() }
            var dragging by remember { mutableStateOf<Float?>(null) }
            val shown = dragging ?: if (useSystem) vm.systemVolume else (pb?.volume ?: 0).toFloat()
            Slider(
                shown,
                { v -> dragging = v; if (useSystem) vm.changeSystemVolume(v) },
                onValueChangeFinished = { dragging?.let { if (!useSystem) vm.setVolume(it.toInt()) }; dragging = null },
                valueRange = 0f..100f, modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = m.primary, activeTrackColor = m.primary, inactiveTrackColor = m.outline),
            )
            RoundButton(Icons.Rounded.Devices, "Devices", onDevices, size = 40.dp)
        }
        pb?.deviceName?.takeIf { it.isNotEmpty() }?.let { Text("Playing on $it", color = m.primary, fontSize = 12.sp) }
        if (!(showLyrics && vm.lyricsEnabled)) Spacer(Modifier.weight(1f))
    }
    }
}
