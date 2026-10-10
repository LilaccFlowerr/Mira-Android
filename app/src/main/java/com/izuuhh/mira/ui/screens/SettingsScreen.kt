package com.izuuhh.mira.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.izuuhh.mira.MiraViewModel
import com.izuuhh.mira.ui.theme.Mira

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
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Appearance", color = m.primary, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Dark theme", color = m.text, modifier = Modifier.weight(1f))
                    Switch(vm.dark, { vm.updateDark(it) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Violet", "Green", "Peach").forEachIndexed { i, name ->
                        FilterChip(vm.accent == i, { vm.updateAccent(i) }, label = { Text(name) })
                    }
                }
            }
        }
        item { Text("Independent Spotify companion. Not affiliated with Spotify.", color = m.muted, fontSize = 11.sp) }
    }
}
