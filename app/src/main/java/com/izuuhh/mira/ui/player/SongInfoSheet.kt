package com.izuuhh.mira.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
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
import com.izuuhh.mira.data.Track
import com.izuuhh.mira.ui.components.Cover
import com.izuuhh.mira.ui.theme.Mira
import com.izuuhh.mira.ui.theme.corner

/** Album, year and more songs by the artist of the song that is playing. [onNavigate] runs after opening an album or artist. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongInfoSheet(vm: MiraViewModel, track: Track, onClose: () -> Unit, onNavigate: () -> Unit) {
    val m = Mira.colors
    LaunchedEffect(track.uri) { vm.loadSongInfo(track) }
    val info = vm.songInfo?.takeIf { it.track.uri == track.uri }
    fun close() { vm.closeSongInfo(); onClose() }
    ModalBottomSheet(onDismissRequest = ::close, containerColor = m.elevated, contentColor = m.text) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Cover(track.cover, 72.dp, radius = 16.dp)
                Column(Modifier.weight(1f)) {
                    Text(track.name, color = m.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(track.subtitle, color = m.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (info == null || info.loading) {
                Text("Loading…", color = m.muted)
            } else {
                info.album?.let { album ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(corner(14.dp))).clickable { vm.openAlbum(album); close(); onNavigate() }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Cover(album.cover, 52.dp, radius = 12.dp)
                        Column(Modifier.weight(1f)) {
                            Text(album.name, color = m.text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val year = info.releaseDate.take(4)
                            val songs = if (info.albumSongs > 0) "${info.albumSongs} songs" else ""
                            Text(listOf(year, songs).filter { it.isNotEmpty() }.joinToString(" · ").ifEmpty { "Album" }, color = m.muted, fontSize = 13.sp)
                        }
                        Icon(Icons.Rounded.ChevronRight, null, tint = m.muted)
                    }
                }
                if (info.more.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(corner(10.dp))).clickable { info.artist?.let { vm.searchArtist(it); close(); onNavigate() } },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("More from ${info.artistName}", color = m.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        if (info.artist != null) Text("See all", color = m.muted, fontSize = 13.sp)
                    }
                    info.more.forEach { t ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(corner(12.dp))).clickable { vm.play(t.uri); close() }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Cover(t.cover, 44.dp, radius = 10.dp)
                            Column(Modifier.weight(1f)) {
                                Text(t.name, color = m.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(t.album, color = m.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}
