package com.izuuhh.mira.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.izuuhh.mira.MiraViewModel
import com.izuuhh.mira.ui.components.Cover
import com.izuuhh.mira.ui.components.RoundButton
import com.izuuhh.mira.ui.components.WavyProgress
import com.izuuhh.mira.ui.components.formatTime
import com.izuuhh.mira.ui.theme.Mira
import kotlinx.coroutines.delay

@Composable
fun NowPlaying(vm: MiraViewModel, onClose: () -> Unit, onDevices: () -> Unit) {
    val m = Mira.colors
    val pb = vm.playback
    var now by remember { mutableLongStateOf(android.os.SystemClock.elapsedRealtime()) }
    LaunchedEffect(pb?.playing) { while (pb?.playing == true) { now = android.os.SystemClock.elapsedRealtime(); delay(100) } }
    Column(
        Modifier.fillMaxSize().background(m.base).windowInsetsPadding(WindowInsets.systemBars).padding(20.dp).clickable(enabled = false) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth()) { RoundButton(Icons.Rounded.KeyboardArrowDown, "Close", onClose, selected = true) }
        Spacer(Modifier.weight(1f))
        Cover(pb?.track?.cover, 300.dp, radius = 32.dp)
        Spacer(Modifier.height(24.dp))
        Text(pb?.track?.name ?: "Nothing playing", color = m.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(pb?.track?.subtitle ?: "Choose a song to start", color = m.muted, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        pb?.track?.album?.takeIf { it.isNotEmpty() }?.let { Text(it, color = m.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        Spacer(Modifier.height(16.dp))
        val position = pb?.position(now) ?: 0
        WavyProgress(position, pb?.track?.durationMs ?: 0, pb?.playing == true, seekable = pb?.allows("seeking") == true, onSeek = { vm.seek(it) }, height = 28.dp)
        Row(Modifier.fillMaxWidth()) {
            Text(formatTime(position), color = m.muted, fontSize = 11.sp)
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
            var volume by remember(pb?.volume) { mutableFloatStateOf((pb?.volume ?: 0).toFloat()) }
            Slider(volume, { volume = it }, onValueChangeFinished = { vm.setVolume(volume.toInt()) }, valueRange = 0f..100f, enabled = pb?.supportsVolume == true, modifier = Modifier.weight(1f))
            RoundButton(Icons.Rounded.Devices, "Devices", onDevices, size = 40.dp)
        }
        pb?.deviceName?.takeIf { it.isNotEmpty() }?.let { Text("Playing on $it", color = m.primary, fontSize = 12.sp) }
        Spacer(Modifier.weight(1f))
    }
}
