package com.izuuhh.mira.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.izuuhh.mira.Page
import com.izuuhh.mira.ui.theme.Mira

@Composable
fun NavBar(vm: MiraViewModel) {
    val m = Mira.colors
    val items = listOf(
        Triple(Page.Home, "Home", Icons.Rounded.Home),
        Triple(Page.Search, "Search", Icons.Rounded.Search),
        Triple(Page.Liked, "Liked", Icons.Rounded.Favorite),
        Triple(Page.Playlists, "Playlists", Icons.Rounded.LibraryMusic),
        Triple(Page.Settings, "Settings", Icons.Rounded.Settings),
    )
    val current = if (vm.page == Page.Playlist) Page.Playlists else vm.page
    Row(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(24.dp)).background(m.panel).padding(6.dp)) {
        items.forEach { (page, label, icon) ->
            val active = page == current
            Column(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(16.dp)).clickable {
                    if (page == Page.Search) { vm.navigate(Page.Search); if (vm.searchTerm.isNotBlank()) vm.search(vm.searchTerm) } else vm.navigate(page)
                },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier.width(if (active) 56.dp else 40.dp).height(32.dp).clip(RoundedCornerShape(16.dp)).background(if (active) m.container else androidx.compose.ui.graphics.Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, label, tint = if (active) m.primary else m.muted, modifier = Modifier.size(22.dp)) }
                Text(label, color = if (active) m.text else m.muted, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}
