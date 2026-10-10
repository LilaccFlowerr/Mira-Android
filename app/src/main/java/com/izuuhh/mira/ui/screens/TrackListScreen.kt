package com.izuuhh.mira.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
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
import com.izuuhh.mira.ui.components.formatTime
import com.izuuhh.mira.ui.theme.Mira

/** Search results, Liked songs and a single playlist share one layout: header, actions, filter, songs. */
@Composable
fun TrackListScreen(vm: MiraViewModel) {
    val m = Mira.colors
    val page = vm.page
    var filter by remember(page) { mutableStateOf("") }
    val shown = remember(vm.items, filter) {
        val t = filter.trim().lowercase()
        if (t.isEmpty()) vm.items else vm.items.filter { listOf(it.name, it.subtitle, it.album).any { s -> s.lowercase().contains(t) } }
    }
    val playlist = if (page == Page.Album) vm.selectedAlbum else vm.selectedPlaylist
    val currentUri = vm.playback?.track?.uri
    val playing = vm.playback?.playing == true
    val canFilter = page == Page.Liked || page == Page.Playlist || page == Page.Album
    val isContext = page == Page.Playlist || page == Page.Album

    fun playRow(index: Int) {
        when (page) {
            Page.Playlist, Page.Album -> vm.playInContext(playlist?.uri.orEmpty(), shown[index].uri)
            Page.Liked -> vm.playLiked(shown.map { it.uri }, index)
            else -> vm.playFrom(shown.map { it.uri }, index)
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    RoundButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", { vm.back() }, selected = true, size = 40.dp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    when (page) {
                        Page.Liked -> Box(Modifier.size(104.dp).clip(RoundedCornerShape(corner(24.dp))).background(m.container), Alignment.Center) {
                            ShapeArt(3, m.primary.copy(alpha = 0.25f), 86.dp)
                            Icon(Icons.Rounded.Favorite, null, tint = m.primary, modifier = Modifier.size(38.dp))
                        }
                        Page.Playlist, Page.Album -> Cover(playlist?.cover ?: vm.items.firstOrNull()?.cover, 104.dp, radius = 20.dp)
                        else -> Box(Modifier.size(104.dp).clip(RoundedCornerShape(corner(24.dp))).background(m.container), Alignment.Center) {
                            ShapeArt(1, m.primary.copy(alpha = 0.3f), 86.dp)
                            Icon(Icons.Rounded.Search, null, tint = m.primary, modifier = Modifier.size(36.dp))
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            when (page) { Page.Liked -> "LIBRARY"; Page.Playlist -> "PLAYLIST"; Page.Album -> "ALBUM"; else -> "SEARCH RESULTS" },
                            color = m.primary, fontSize = 10.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold,
                        )
                        Text(
                            when (page) { Page.Liked -> "Liked songs"; Page.Playlist, Page.Album -> playlist?.name ?: "Playlist"; else -> if (vm.searchTerm.isNotBlank()) "‘${vm.searchTerm}’" else "Search" },
                            color = m.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 30.sp,
                        )
                        Text(
                            when (page) { Page.Liked -> "Songs you saved on Spotify."; Page.Playlist, Page.Album -> playlist?.subtitle?.ifEmpty { null } ?: "Spotify"; else -> "Songs, artists, albums and playlists." },
                            color = m.muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (canFilter) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { if (isContext) vm.play(playlist?.uri) else vm.playLiked(shown.map { it.uri }, 0) },
                            enabled = vm.connected && (isContext || shown.isNotEmpty()),
                            colors = ButtonDefaults.buttonColors(containerColor = m.primary, contentColor = m.onPrimary),
                        ) { Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Play") }
                        if (page == Page.Liked) FilledTonalButton(
                            onClick = { vm.playLiked(shown.map { it.uri }, 0, shuffle = true) }, enabled = shown.isNotEmpty(),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = m.container, contentColor = m.primary),
                        ) { Icon(Icons.Rounded.Shuffle, null); Spacer(Modifier.width(6.dp)); Text("Shuffle") }
                        if (isContext) FilledTonalButton(
                            onClick = { vm.openSpotify(playlist?.url.orEmpty()) },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = m.container, contentColor = m.primary),
                        ) { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null); Spacer(Modifier.width(6.dp)); Text("Spotify") }
                    }
                    FilterField(filter, { filter = it }, when (page) { Page.Liked -> "Find in liked songs"; Page.Album -> "Find in album"; else -> "Find in playlist" })
                }
            }
        }
        if (page == Page.Search) {
            if (vm.searchTerm.isBlank()) item(key = "recents") { RecentSearches(vm) }
            else {
                if (vm.searchArtists.isNotEmpty()) item(key = "artists") { ArtistRow(vm.searchArtists) { vm.searchArtist(it) } }
                if (vm.searchAlbums.isNotEmpty()) item(key = "albums") { CardRow("Albums", vm.searchAlbums) { vm.openAlbum(it) } }
                if (vm.searchPlaylists.isNotEmpty()) item(key = "playlists") { CardRow("Playlists", vm.searchPlaylists) { vm.openPlaylist(it) } }
                if (vm.items.isNotEmpty()) item(key = "songs-title") { SectionTitle("Songs", Modifier.padding(top = 10.dp, bottom = 4.dp)) }
            }
        }
        if (!vm.busy && vm.loadError && vm.items.isEmpty()) item(key = "error") {
            StatusCard("Could not load songs", vm.info, onRetry = { vm.retry() }, error = true)
        }
        if (!vm.busy && !vm.loadError && shown.isEmpty() && !(page == Page.Search && (vm.searchTerm.isBlank() || vm.searchArtists.isNotEmpty() || vm.searchAlbums.isNotEmpty() || vm.searchPlaylists.isNotEmpty()))) item(key = "empty") {
            StatusCard(
                if (!vm.connected) "Not connected" else if (filter.isNotBlank()) "No match" else "Nothing here",
                when {
                    !vm.connected -> "Connect Spotify in Settings to see your songs."
                    filter.isNotBlank() -> "No loaded songs match ‘$filter’." + if (vm.hasMore) " Load more to search further." else ""
                    page == Page.Search && vm.searchTerm.isBlank() -> "Type something in the search bar."
                    else -> vm.info.ifEmpty { "Spotify returned no songs for this view." }
                },
            )
        }
        if (shown.isNotEmpty() && page != Page.Search) item(key = "count") {
            Text(
                if (shown.size != vm.items.size) "${shown.size} of ${vm.items.size} loaded" else "${vm.items.size} songs loaded",
                color = m.muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            )
        }
        itemsIndexed(shown, key = { i, t -> "${t.uri}#$i" }) { index, t ->
            val current = t.uri == currentUri
            TrackRow(
                track = t, number = index + 1, current = current, playing = current && playing,
                modifier = Modifier.animateItem(),
                onClick = { if (current) vm.togglePlayback() else playRow(index) },
                onPlayOnly = { vm.play(t.uri) }, onQueue = { vm.queue(t.uri) }, onSave = { vm.save(t.uri) },
                onRemove = if (page == Page.Liked) ({ vm.save(t.uri, remove = true) }) else null,
                onOpen = { vm.openSpotify(t.url) },
            )
        }
        if (vm.hasMore && shown.isNotEmpty()) item(key = "more") {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp), Alignment.Center) {
                FilledTonalButton(
                    onClick = { vm.loadMore() }, enabled = !vm.busy,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = m.container, contentColor = m.primary),
                ) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text(if (vm.busy) "Loading…" else "Load more") }
            }
        }
        if (shown.isNotEmpty()) item(key = "footer") {
            Text("Content provided by Spotify · open in Spotify for all features", color = m.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 14.dp, start = 6.dp))
        }
    }
}

@Composable
private fun FilterField(value: String, onChange: (String) -> Unit, placeholder: String) {
    val m = Mira.colors
    Row(
        Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(corner(22.dp))).background(m.elevated).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = m.muted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value, onChange, singleLine = true, modifier = Modifier.weight(1f),
            textStyle = TextStyle(color = m.text, fontSize = 14.sp), cursorBrush = SolidColor(m.primary),
            decorationBox = { inner -> Box(contentAlignment = Alignment.CenterStart) { if (value.isEmpty()) Text(placeholder, color = m.muted, fontSize = 14.sp); inner() } },
        )
        if (value.isNotEmpty()) Icon(Icons.Rounded.Close, "Clear filter", tint = m.muted, modifier = Modifier.size(18.dp).clickable { onChange("") })
    }
}

@Composable
private fun TrackRow(
    track: Track, number: Int, current: Boolean, playing: Boolean, modifier: Modifier,
    onClick: () -> Unit, onPlayOnly: () -> Unit, onQueue: () -> Unit, onSave: () -> Unit, onRemove: (() -> Unit)?, onOpen: () -> Unit,
) {
    val m = Mira.colors
    var menu by remember { mutableStateOf(false) }
    val background by animateColorAsState(if (current) m.container else androidx.compose.ui.graphics.Color.Transparent, label = "row")
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(corner(16.dp))).background(background).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(30.dp), Alignment.Center) {
            if (current) Icon(if (playing) Icons.Rounded.GraphicEq else Icons.Rounded.Pause, null, tint = m.primary, modifier = Modifier.size(20.dp))
            else Text(number.toString(), color = m.muted, fontSize = 12.sp)
        }
        Cover(track.cover, 46.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(track.name, color = if (current) m.primary else m.text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.subtitle, color = m.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (track.durationMs > 0) Text(formatTime(track.durationMs), color = m.muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp))
        Box {
            IconButton({ menu = true }) { Icon(Icons.Rounded.MoreVert, "More actions for ${track.name}", tint = m.muted) }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem({ Text("Play only this song") }, { menu = false; onPlayOnly() })
                DropdownMenuItem({ Text("Add to queue") }, { menu = false; onQueue() })
                DropdownMenuItem({ Text("Save to library") }, { menu = false; onSave() })
                DropdownMenuItem({ Text("Open in Spotify") }, { menu = false; onOpen() })
                onRemove?.let { DropdownMenuItem({ Text("Remove from library") }, { menu = false; it() }) }
            }
        }
    }
}

@Composable
private fun RecentSearches(vm: MiraViewModel) {
    val m = Mira.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (vm.recents.isEmpty()) {
            StatusCard("Search Spotify", "Type a song, artist, album or playlist in the search bar above.")
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Recent searches", Modifier.weight(1f))
                TextButton({ vm.clearRecents() }) { Text("Clear", color = m.primary) }
            }
            vm.recents.forEach { term ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(corner(14.dp))).clickable { vm.search(term) }.padding(horizontal = 6.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.History, null, tint = m.muted)
                    Spacer(Modifier.width(14.dp))
                    Text(term, color = m.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Rounded.NorthWest, null, tint = m.muted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ArtistRow(artists: List<Track>, onClick: (Track) -> Unit) {
    val m = Mira.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Artists")
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(artists.size) { i ->
                val a = artists[i]
                Column(Modifier.width(88.dp).clip(RoundedCornerShape(corner(14.dp))).clickable { onClick(a) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(80.dp).clip(androidx.compose.foundation.shape.CircleShape)) { Cover(a.cover, 80.dp, radius = 40.dp) }
                    Text(a.name, color = m.text, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun CardRow(title: String, entries: List<Track>, onClick: (Track) -> Unit) {
    val m = Mira.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(title)
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(entries.size) { i ->
                val t = entries[i]
                Column(Modifier.width(132.dp).clip(RoundedCornerShape(corner(16.dp))).clickable { onClick(t) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Cover(t.cover, 132.dp, radius = 18.dp)
                    Text(t.name, color = m.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(t.subtitle, color = m.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
