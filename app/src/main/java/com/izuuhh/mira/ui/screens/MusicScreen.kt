package com.izuuhh.mira.ui.screens

import com.izuuhh.mira.ui.theme.miraSurface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.izuuhh.mira.MiraViewModel
import com.izuuhh.mira.ui.theme.corner
import com.izuuhh.mira.Page
import com.izuuhh.mira.ui.components.ShapeArt
import com.izuuhh.mira.ui.theme.Mira

/** The music pages: Home, Playlists, and the track lists (Search, Liked songs, one playlist). */
@Composable
fun MusicScreen(vm: MiraViewModel, openPlayer: () -> Unit, goSettings: () -> Unit) {
    when (vm.page) {
        Page.Home -> HomeScreen(vm, openPlayer, goSettings)
        Page.Playlists -> PlaylistsScreen(vm)
        else -> TrackListScreen(vm)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, color = Mira.colors.text, fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = modifier)
}

/** Shown when a list is empty or could not be loaded. */
@Composable
fun StatusCard(title: String, body: String, onRetry: (() -> Unit)? = null, error: Boolean = false) {
    val m = Mira.colors
    Row(
        Modifier.fillMaxWidth().miraSurface(RoundedCornerShape(corner(22.dp)), m.elevated).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShapeArt(1, if (error) m.error else m.primary, 52.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = if (error) m.error else m.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(body, color = m.muted, fontSize = 12.sp)
        }
        if (onRetry != null) IconButton(onRetry) { Icon(Icons.Rounded.Refresh, "Try again", tint = m.primary) }
    }
}
