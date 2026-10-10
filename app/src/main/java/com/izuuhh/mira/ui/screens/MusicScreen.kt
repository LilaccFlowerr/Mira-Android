package com.izuuhh.mira.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.izuuhh.mira.MiraViewModel
import com.izuuhh.mira.Page
import com.izuuhh.mira.data.Track
import com.izuuhh.mira.ui.components.Cover
import com.izuuhh.mira.ui.components.RoundButton
import com.izuuhh.mira.ui.theme.Mira

@Composable
fun MusicScreen(vm: MiraViewModel, openPlayer: () -> Unit, goSettings: () -> Unit) {
    val m = Mira.colors
    val list = when (vm.page) {
        Page.Home -> vm.playlists
        Page.Playlists -> vm.playlists
        else -> vm.items
    }
    val trackList = vm.page == Page.Search || vm.page == Page.Liked || vm.page == Page.Playlist
    var filter by remember(vm.page) { mutableStateOf("") }
    val shown = if (trackList && filter.isNotBlank()) {
        val t = filter.trim().lowercase()
        list.filter { listOf(it.name, it.subtitle, it.album).any { s -> s.lowercase().contains(t) } }
    } else list

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (vm.page == Page.Home) item { Hero(vm, openPlayer, goSettings) }
        if (vm.page == Page.Playlist) item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                RoundButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", { vm.back() }, selected = true, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Text(vm.selectedPlaylist?.name.orEmpty(), color = m.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        } else if (vm.page != Page.Home) item {
            Text(
                when (vm.page) { Page.Search -> "Search"; Page.Liked -> "Liked songs"; else -> "Playlists" },
                color = m.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        if (vm.page == Page.Home) item { Text("YOUR PLAYLISTS", color = m.muted, fontSize = 10.sp, letterSpacing = 1.4.sp, modifier = Modifier.padding(top = 8.dp)) }
        if (trackList && (vm.page == Page.Liked || vm.page == Page.Playlist)) item {
            OutlinedTextField(filter, { filter = it }, singleLine = true, placeholder = { Text("Filter loaded songs") }, modifier = Modifier.fillMaxWidth())
        }
        if (vm.page == Page.Liked && vm.items.isNotEmpty()) item {
            Button(onClick = { vm.playLiked(vm.items.map { it.uri }, 0, shuffle = true) }, colors = ButtonDefaults.buttonColors(containerColor = m.primary, contentColor = m.onPrimary)) {
                Icon(Icons.Rounded.Shuffle, null); Spacer(Modifier.width(8.dp)); Text("Shuffle play")
            }
        }
        if (vm.busy && list.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator(color = m.primary) } }
        if (!vm.busy && shown.isEmpty()) item {
            Text(
                when {
                    !vm.connected -> "Connect Spotify in Settings to see your music."
                    vm.page == Page.Search && vm.items.isEmpty() && vm.searchTerm.isBlank() -> "Type something in the search bar."
                    vm.info.isNotEmpty() -> vm.info
                    else -> "Nothing here yet."
                },
                color = m.muted, modifier = Modifier.padding(vertical = 16.dp),
            )
        }
        itemsIndexed(shown, key = { i, t -> "${t.uri}#$i" }) { index, t ->
            TrackRow(t, onClick = {
                when {
                    t.type == "playlist" -> vm.openPlaylist(t)
                    vm.page == Page.Playlist -> vm.playInContext(vm.selectedPlaylist?.uri.orEmpty(), t.uri)
                    vm.page == Page.Liked -> vm.playLiked(shown.map { it.uri }, index)
                    else -> vm.playFrom(shown.map { it.uri }, index)
                }
            }, onQueue = if (t.type == "track") ({ vm.queue(t.uri) }) else null, onSave = if (t.type == "track") ({ vm.save(t.uri) }) else null)
        }
        if (vm.hasMore && trackList) item { TextButton(onClick = { vm.loadMore() }) { Text("Load more", color = m.primary) } }
    }
}

@Composable
private fun Hero(vm: MiraViewModel, openPlayer: () -> Unit, goSettings: () -> Unit) {
    val m = Mira.colors
    val pb = vm.playback
    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greeting = when { hour < 5 -> "Good night"; hour < 12 -> "Good morning"; hour < 18 -> "Good afternoon"; else -> "Good evening" }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(m.container).padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (pb != null) (if (pb.playing) "NOW PLAYING" else "PAUSED") else greeting.uppercase(), color = m.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
            Text(
                pb?.track?.name ?: if (!vm.connected) "Connect Spotify to get started." else "$greeting.",
                color = m.text, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 28.sp,
            )
            Text(pb?.track?.subtitle ?: if (vm.connected) "Pick something from your library or search above." else "", color = m.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { if (pb != null) openPlayer() else if (vm.connected) vm.navigate(Page.Liked) else goSettings() },
                colors = ButtonDefaults.buttonColors(containerColor = m.primary, contentColor = m.onPrimary),
            ) { Text(if (pb != null) "Open player" else if (vm.connected) "Liked songs" else "Connect Spotify") }
        }
        if (pb != null) { Spacer(Modifier.width(12.dp)); Cover(pb.track.cover, 96.dp, radius = 24.dp) }
    }
}

@Composable
private fun TrackRow(t: Track, onClick: () -> Unit, onQueue: (() -> Unit)?, onSave: (() -> Unit)?) {
    val m = Mira.colors
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(t.cover, 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.name, color = m.text, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text(if (t.type == "playlist") "Playlist · ${t.subtitle}" else t.subtitle, color = m.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onQueue != null || onSave != null) {
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "More", tint = m.muted) }
                DropdownMenu(menu, { menu = false }) {
                    onQueue?.let { DropdownMenuItem({ Text("Add to queue") }, { menu = false; it() }) }
                    onSave?.let { DropdownMenuItem({ Text("Save to your library") }, { menu = false; it() }) }
                }
            }
        }
    }
}
