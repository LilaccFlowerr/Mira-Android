package com.izuuhh.mira.ui.screens

import com.izuuhh.mira.ui.theme.miraSurface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.izuuhh.mira.SettingsViewModel
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.izuuhh.mira.MiraViewModel
import com.izuuhh.mira.ui.theme.corner
import com.izuuhh.mira.Page
import com.izuuhh.mira.ui.components.Cover
import com.izuuhh.mira.ui.components.ShapeArt
import com.izuuhh.mira.ui.theme.Mira

private class Genre(val name: String, val query: String, val bg: Long, val ink: Long, val shape: Int)

private val Genres = listOf(
    Genre("Ambient", "ambient", 0xFF343b33, 0xFFc3d6ad, 1), Genre("Electronic", "electronic", 0xFF44313e, 0xFFefbad5, 2),
    Genre("Indie", "indie", 0xFF40372f, 0xFFedc6a0, 0), Genre("Jazz", "jazz", 0xFF303b48, 0xFFb6cdec, 3),
)

@Composable
fun HomeScreen(vm: MiraViewModel, openPlayer: () -> Unit, goSettings: () -> Unit) {
    val settings: SettingsViewModel = viewModel()
    val m = Mira.colors
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { Hero(vm, openPlayer, goSettings) }
        if (settings.showGenres) item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Browse by genre")
                Genres.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { g -> GenreCard(g, Modifier.weight(1f)) { vm.searchTerm = g.query; vm.search(g.query) } }
                    }
                }
            }
        }
        if (vm.connected) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Your playlists", Modifier.weight(1f))
                    androidx.compose.material3.TextButton({ vm.navigate(Page.Playlists) }) { Text("See all", color = m.primary) }
                }
            }
            playlistGrid(vm, limit = 5)
        }
        item { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun GenreCard(g: Genre, modifier: Modifier, onClick: () -> Unit) {
    val ink = Color(g.ink)
    var pressed by remember { mutableStateOf(false) }
    val turn by androidx.compose.animation.core.animateFloatAsState(if (pressed) 25f else 0f, label = "turn")
    Box(
        modifier.height(108.dp).clip(RoundedCornerShape(corner(24.dp))).background(Color(g.bg))
            .clickable { pressed = !pressed; onClick() },
    ) {
        ShapeArt(g.shape, ink, 56.dp, Modifier.align(Alignment.TopEnd).padding(10.dp), turn)
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(g.name, color = ink, fontWeight = FontWeight.Bold)
            Text("Search", color = ink.copy(alpha = 0.7f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun Hero(vm: MiraViewModel, openPlayer: () -> Unit, goSettings: () -> Unit) {
    val m = Mira.colors
    val pb = vm.playback
    // The decorative shape turns slowly while music plays.
    val spin = rememberInfiniteTransition(label = "spin")
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(40000, easing = LinearEasing), RepeatMode.Restart), label = "angle")
    // The text gets the full width under the artwork: beside it the title always had to wrap.
    Column(
        Modifier.fillMaxWidth().miraSurface(RoundedCornerShape(corner(26.dp)), m.container).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(
                if (pb != null) (if (pb.playing) "NOW PLAYING" else "PAUSED") else "NOT PLAYING",
                color = m.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp, modifier = Modifier.weight(1f),
            )
            // The shape turns slowly while music plays; the cover sits on top of it. It is drawn larger than the
            // room it takes, so it spills over the corner of the card.
            Box(Modifier.size(120.dp), Alignment.Center) {
                ShapeArt(3, m.primary.copy(alpha = if (pb != null) 0.35f else 0.9f), 168.dp, modifier = Modifier.requiredSize(168.dp), turn = if (pb?.playing == true) angle else 0f)
                if (pb != null) Cover(pb.track.cover, 100.dp, radius = 26.dp)
            }
        }
        Text(
            pb?.track?.name ?: if (!vm.connected) "Connect Spotify to get started." else "Nothing playing.",
            color = m.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 32.sp,
        )
        Text(pb?.track?.subtitle ?: if (vm.connected) "Pick something from your library." else "", color = m.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { if (pb != null) openPlayer() else if (vm.connected) vm.navigate(Page.Liked) else goSettings() },
            colors = ButtonDefaults.buttonColors(containerColor = m.primary, contentColor = m.onPrimary),
        ) { Text(if (pb != null) "Open player" else if (vm.connected) "Liked songs" else "Connect Spotify", maxLines = 1, softWrap = false) }
    }
}
