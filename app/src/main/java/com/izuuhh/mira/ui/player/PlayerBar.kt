package com.izuuhh.mira.ui.player

import com.izuuhh.mira.ui.theme.miraSurface
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.izuuhh.mira.MiraViewModel
import com.izuuhh.mira.ui.theme.corner
import com.izuuhh.mira.ui.components.Cover
import com.izuuhh.mira.ui.components.RoundButton
import com.izuuhh.mira.ui.components.WavyProgress
import com.izuuhh.mira.ui.components.rememberPlaybackClock
import com.izuuhh.mira.ui.theme.Mira

@Composable
fun PlayerBar(vm: MiraViewModel, onExpand: () -> Unit, onDevices: () -> Unit) {
    val m = Mira.colors
    val pb = vm.playback
    val clock = rememberPlaybackClock(pb?.playing == true)
    Box(Modifier.fillMaxWidth().height(90.dp).miraSurface(RoundedCornerShape(corner(20.dp)), m.elevated, docked = true).clickable(onClick = onExpand)) {
        Row(Modifier.fillMaxSize().padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 26.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(pb?.track?.cover, 52.dp, radius = 14.dp)
            Spacer(Modifier.width(10.dp))
            // The title slides when the track changes.
            AnimatedContent(
                targetState = pb?.track?.uri to (pb?.track?.name to pb?.track?.subtitle),
                modifier = Modifier.weight(1f),
                transitionSpec = { (slideInVertically(tween(260)) { it / 3 } + fadeIn(tween(260))) togetherWith (slideOutVertically(tween(160)) { -it / 3 } + fadeOut(tween(160))) },
                label = "title",
            ) { (_, names) ->
                Column {
                    Text(names.first ?: "Nothing playing", color = m.text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        names.second ?: if (vm.deviceId.isEmpty()) "Pick a song to start" else "Choose a song to start",
                        color = m.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (pb == null) RoundButton(Icons.Rounded.Devices, "Choose playback device", onDevices)
            else {
                RoundButton(if (pb.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (pb.playing) "Pause" else "Play", { vm.togglePlayback() }, filled = true, enabled = !pb.restricted)
                RoundButton(Icons.Rounded.SkipNext, "Next track", { vm.command("next") }, enabled = pb.allows("skipping_next"))
            }
        }
        WavyProgress(
            position = { pb?.position(clock.value) ?: 0 }, duration = pb?.track?.durationMs ?: 0, playing = pb?.playing == true,
            seekable = false, onSeek = {},
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 14.dp, vertical = 4.dp), height = 16.dp,
        )
    }
}
