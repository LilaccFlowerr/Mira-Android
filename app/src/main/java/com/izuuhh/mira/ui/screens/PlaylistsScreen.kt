package com.izuuhh.mira.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
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
import com.izuuhh.mira.Page
import com.izuuhh.mira.data.Track
import com.izuuhh.mira.ui.components.Cover
import com.izuuhh.mira.ui.components.RoundButton
import com.izuuhh.mira.ui.components.ShapeArt
import com.izuuhh.mira.ui.theme.Mira

@Composable
fun PlaylistsScreen(vm: MiraViewModel) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionTitle("Your playlists") }
        if (!vm.busy && vm.playlists.isEmpty() && vm.loadError) item { StatusCard("Could not load playlists", vm.info, onRetry = { vm.retry() }, error = true) }
        playlistGrid(vm, limit = Int.MAX_VALUE)
        item { Spacer(Modifier.height(4.dp)) }
    }
}

/** Liked songs first (like a pinned playlist), then the user's playlists, two per row. */
fun LazyListScope.playlistGrid(vm: MiraViewModel, limit: Int) {
    val cards: List<Track?> = (if (vm.connected) listOf<Track?>(null) else emptyList()) + vm.playlists.take(limit)
    items(cards.chunked(2).size) { rowIndex ->
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            cards.chunked(2)[rowIndex].forEach { t ->
                Box(Modifier.weight(1f)) { if (t == null) LikedCard(vm) else PlaylistCard(t, vm) }
            }
            if (cards.chunked(2)[rowIndex].size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun LikedCard(vm: MiraViewModel) {
    val m = Mira.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(corner(24.dp))).background(m.container).clickable { vm.navigate(Page.Liked) },
            contentAlignment = Alignment.Center,
        ) {
            ShapeArt(3, m.primary.copy(alpha = 0.25f), 150.dp)
            Icon(Icons.Rounded.Favorite, "Liked songs", tint = m.primary, modifier = Modifier.size(44.dp))
        }
        Text("Liked songs", color = m.text, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
        Text("Your saved tracks", color = m.muted, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun PlaylistCard(t: Track, vm: MiraViewModel) {
    val m = Mira.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(corner(24.dp))).clickable { vm.openPlaylist(t) }) {
            BoxWithConstraints(Modifier.fillMaxSize()) { Cover(t.cover, maxWidth, radius = 24.dp) }
            RoundButton(Icons.Rounded.PlayArrow, "Play ${t.name}", { vm.play(t.uri) }, Modifier.align(Alignment.BottomEnd).padding(8.dp), filled = true, size = 44.dp)
        }
        Text(t.name, color = m.text, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(t.subtitle.ifEmpty { "Playlist" }, color = m.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
