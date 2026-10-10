package com.izuuhh.mira.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.izuuhh.mira.MiraViewModel
import com.izuuhh.mira.Page
import com.izuuhh.mira.SettingsViewModel
import com.izuuhh.mira.ui.theme.Mira
import com.izuuhh.mira.ui.theme.corner
import com.izuuhh.mira.ui.theme.miraSurface

class NavTab(val label: String, val icon: ImageVector)

/** Name and icon of a page as shown in the bar and in the tab settings. */
fun navTab(page: Page) = when (page) {
    Page.Home -> NavTab("Home", Icons.Rounded.Home)
    Page.Search -> NavTab("Search", Icons.Rounded.Search)
    Page.Liked -> NavTab("Liked", Icons.Rounded.Favorite)
    Page.Playlists -> NavTab("Playlists", Icons.Rounded.LibraryMusic)
    else -> NavTab("Settings", Icons.Rounded.Settings)
}

@Composable
fun NavBar(vm: MiraViewModel) {
    val settings: SettingsViewModel = viewModel()
    val m = Mira.colors
    val current = if (vm.page == Page.Playlist) Page.Playlists else vm.page
    val tabs = settings.visibleTabs
    val labels = settings.navLabels
    val indicator = settings.navIndicator

    // Fixed row heights, so nothing jumps when a label shows or hides; the indicator is placed from the same numbers.
    // The spacing above the icon comes from the target height: using the animated one made it negative mid-animation.
    val iconH = if (indicator == 1) 36.dp else 32.dp
    val labelH = if (labels == 2) 0.dp else 18.dp
    val underlineH = if (indicator == 2) 5.dp else 0.dp
    val targetH = if (labels == 2) 60.dp else 78.dp
    val barH by animateDpAsState(targetH, spring(stiffness = Spring.StiffnessMediumLow), label = "bar-height")
    val topPad = ((targetH - 12.dp - iconH - labelH - underlineH) / 2).coerceAtLeast(0.dp)

    BoxWithConstraints(Modifier.fillMaxWidth().height(barH).miraSurface(RoundedCornerShape(corner(24.dp)), m.panel, docked = true).padding(6.dp)) {
        val tabW = maxWidth / tabs.size
        val index = tabs.indexOf(current)
        val spring = spring<androidx.compose.ui.unit.Dp>(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
        val indW = if (indicator == 2) 20.dp else if (indicator == 1) 36.dp else 56.dp
        val x by animateDpAsState(tabW * index.coerceAtLeast(0) + (tabW - indW) / 2, spring, label = "indicator-x")
        val shown by animateFloatAsState(if (index >= 0) 1f else 0f, label = "indicator-shown")

        // One indicator that slides between the tabs.
        if (indicator == 2) {
            Box(Modifier.offset(x, topPad + iconH + 2.dp).width(indW).height(3.dp).graphicsLayer { alpha = shown }.clip(CircleShape).background(m.primary))
        } else {
            Box(
                Modifier.offset(x, topPad).width(indW).height(iconH).graphicsLayer { alpha = shown }
                    .clip(if (indicator == 1) CircleShape else RoundedCornerShape(corner(16.dp))).background(m.container),
            )
        }

        Row(Modifier.fillMaxSize()) {
            tabs.forEach { page ->
                val tab = navTab(page)
                val active = page == current
                val scale by animateFloatAsState(if (active) 1.18f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "icon-scale")
                val tint by animateColorAsState(if (active) m.primary else m.muted, label = "icon-colour")
                val labelColour by animateColorAsState(if (active) m.text else m.muted, label = "label-colour")
                val labelShown by animateFloatAsState(if (labels == 0 || active) 1f else 0f, label = "label-shown")
                Column(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(corner(16.dp))).clickable {
                        if (page == Page.Search) { vm.navigate(Page.Search); if (vm.searchTerm.isNotBlank()) vm.search(vm.searchTerm) } else vm.navigate(page)
                    }.padding(top = topPad),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.height(iconH).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Icon(tab.icon, tab.label, tint = tint, modifier = Modifier.size(22.dp).graphicsLayer { scaleX = scale; scaleY = scale })
                    }
                    Spacer(Modifier.height(underlineH))
                    if (labels != 2) {
                        // With "Active" the label rises into place under the icon instead of popping in.
                        Box(Modifier.height(labelH), contentAlignment = Alignment.Center) {
                            Text(
                                tab.label, color = labelColour, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1, softWrap = false,
                                // Unbounded, so a larger text size can never be cut off by the slot it sits in.
                                modifier = Modifier.wrapContentHeight(unbounded = true).graphicsLayer { alpha = labelShown; translationY = (1f - labelShown) * 6.dp.toPx() },
                            )
                        }
                    }
                }
            }
        }
    }
}
