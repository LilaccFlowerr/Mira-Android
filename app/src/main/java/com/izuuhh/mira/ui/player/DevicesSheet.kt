package com.izuuhh.mira.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.izuuhh.mira.MiraViewModel
import com.izuuhh.mira.ui.theme.Mira

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesSheet(vm: MiraViewModel, onClose: () -> Unit) {
    val m = Mira.colors
    LaunchedEffect(Unit) { vm.refreshPlayer(true) }
    ModalBottomSheet(onDismissRequest = onClose, containerColor = m.elevated, contentColor = m.text) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Where do you want to listen?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = m.text)
            Button(onClick = { vm.openSpotifyApp(); onClose() }, enabled = vm.connected, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = m.primary, contentColor = m.onPrimary)) {
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, null); Spacer(Modifier.width(8.dp)); Text("Open Spotify on this phone")
            }
            if (vm.devices.isEmpty()) Text("No devices found. Open Spotify on this phone or another device.", color = m.muted)
            vm.devices.forEach { d ->
                val selected = d.id == (vm.playback?.deviceId?.takeIf { it.isNotEmpty() } ?: vm.deviceId)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (selected) m.container else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { vm.selectDevice(d.id); onClose() }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(if (d.type.equals("Smartphone", true)) Icons.Rounded.PhoneAndroid else Icons.Rounded.Speaker, null, tint = if (selected) m.primary else m.muted)
                    Spacer(Modifier.width(12.dp))
                    Text(d.name, color = m.text, modifier = Modifier.weight(1f))
                    if (selected) Icon(Icons.Rounded.Check, null, tint = m.primary)
                }
            }
        }
    }
}
