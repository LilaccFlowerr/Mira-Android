package com.izuuhh.mira.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.izuuhh.mira.MiraViewModel
import com.izuuhh.mira.data.LyricsState
import com.izuuhh.mira.ui.theme.Mira
import com.izuuhh.mira.ui.theme.corner

/** Lyrics in place of the cover: synced lines follow the song and you can tap a line to jump there. */
@Composable
fun LyricsPanel(vm: MiraViewModel, positionMs: () -> Long, modifier: Modifier = Modifier) {
    val m = Mira.colors
    val state = vm.lyrics
    Box(modifier, Alignment.Center) {
        when (state) {
            LyricsState.Idle, LyricsState.Loading -> Message("Loading lyrics…")
            LyricsState.NotFound -> Message("No lyrics found for this song.")
            LyricsState.Failed -> Message("Could not reach the lyrics service. Check your connection.")
            is LyricsState.Found -> {
                val lyrics = state.lyrics
                if (lyrics.synced.isNotEmpty()) {
                    val list = rememberLazyListState()
                    // The line that is being sung: the last one whose time has passed.
                    val current by remember(lyrics) {
                        derivedStateOf { lyrics.synced.indexOfLast { it.timeMs <= positionMs() + 250 }.coerceAtLeast(0) }
                    }
                    LaunchedEffect(current) { list.animateScrollToItem(current, scrollOffset = -220) }
                    LazyColumn(Modifier.fillMaxSize(), list, contentPadding = PaddingValues(vertical = 120.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(lyrics.synced) { i, line ->
                            val active = i == current
                            val colour by animateColorAsState(if (active) m.text else m.muted.copy(alpha = 0.7f), label = "line")
                            val scale by animateFloatAsState(if (active) 1f else 0.9f, label = "scale")
                            Text(
                                line.text.ifBlank { "♪" }, color = colour, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(corner(14.dp))).clickable { vm.seek(line.timeMs) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp).graphicsLayer { scaleX = scale; scaleY = scale; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f) },
                            )
                        }
                    }
                } else {
                    Text(
                        lyrics.plain, color = m.text, fontSize = 18.sp, lineHeight = 28.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Message(text: String) {
    Text(text, color = Mira.colors.muted, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
}
