package com.izuuhh.mira.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.izuuhh.mira.MiraViewModel
import com.izuuhh.mira.ui.components.Cover
import com.izuuhh.mira.ui.components.RoundButton
import com.izuuhh.mira.ui.components.WavyProgress
import com.izuuhh.mira.ui.theme.Mira
import kotlinx.coroutines.delay

@Composable
fun PlayerBar(vm: MiraViewModel, onExpand: () -> Unit, onDevices: () -> Unit) {
    val m = Mira.colors
    val pb = vm.playback
    var now by remember { mutableLongStateOf(android.os.SystemClock.elapsedRealtime()) }
    LaunchedEffect(pb?.playing) { while (pb?.playing == true) { now = android.os.SystemClock.elapsedRealtime(); delay(200) } }
    Box(Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(20.dp)).background(m.elevated).clickable(onClick = onExpand)) {
        Row(Modifier.fillMaxSize().padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 26.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(pb?.track?.cover, 52.dp, radius = 14.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(pb?.track?.name ?: "Nothing playing", color = m.text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    pb?.track?.subtitle ?: if (vm.deviceId.isEmpty()) "Pick a song: Mira opens Spotify for you" else "Choose a song to start",
                    color = m.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (pb == null) RoundButton(Icons.Rounded.Devices, "Choose playback device", onDevices)
            else {
                RoundButton(if (pb.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (pb.playing) "Pause" else "Play", { vm.togglePlayback() }, filled = true, enabled = !pb.restricted)
                RoundButton(Icons.Rounded.SkipNext, "Next track", { vm.command("next") }, enabled = pb.allows("skipping_next"))
            }
        }
        WavyProgress(
            pb?.position(now) ?: 0, pb?.track?.durationMs ?: 0, pb?.playing == true, seekable = false, onSeek = {},
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 14.dp, vertical = 4.dp), height = 16.dp,
        )
    }
}
