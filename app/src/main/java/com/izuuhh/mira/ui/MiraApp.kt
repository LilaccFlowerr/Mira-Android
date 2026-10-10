package com.izuuhh.mira.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.izuuhh.mira.Event
import com.izuuhh.mira.MiraViewModel
import com.izuuhh.mira.Page
import com.izuuhh.mira.data.Track
import com.izuuhh.mira.ui.theme.Mira
import kotlinx.coroutines.delay

@Composable
fun MiraApp(vm: MiraViewModel, onEvent: (Event) -> Unit) {
    val m = Mira.colors
    val snackbar = remember { SnackbarHostState() }
    var showPlayer by remember { mutableStateOf(false) }
    var showDevices by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.eventFlow.collect { e ->
            if (e is Event.Message) snackbar.showSnackbar(e.text) else onEvent(e)
        }
    }
    BackHandler(enabled = showPlayer) { showPlayer = false }
    BackHandler(enabled = !showPlayer && vm.page != Page.Home) { vm.back() }

    Box(Modifier.fillMaxSize().background(m.base)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).padding(horizontal = 8.dp)) {
            SearchBar(vm)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(m.panel)) {
                when (vm.page) {
                    Page.Settings -> SettingsScreen(vm)
                    else -> MusicScreen(vm, openPlayer = { showPlayer = true }, goSettings = { vm.navigate(Page.Settings) })
                }
            }
            Spacer(Modifier.height(8.dp))
            PlayerBar(vm, onExpand = { showPlayer = true }, onDevices = { showDevices = true })
            Spacer(Modifier.height(8.dp))
            NavBar(vm)
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 190.dp))
        if (showPlayer) NowPlaying(vm, onClose = { showPlayer = false }, onDevices = { showDevices = true })
        if (showDevices) DevicesSheet(vm, onClose = { showDevices = false })
    }
}

@Composable
private fun SearchBar(vm: MiraViewModel) {
    val m = Mira.colors
    val focus = LocalFocusManager.current
    Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("mira", color = m.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp, end = 12.dp))
        Row(
            Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(22.dp)).background(m.elevated).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = m.muted)
            Spacer(Modifier.width(8.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = vm.searchTerm, onValueChange = { vm.searchTerm = it },
                singleLine = true, modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(color = m.text, fontSize = 15.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(m.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus(); vm.search(vm.searchTerm) }),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (vm.searchTerm.isEmpty()) Text("Search music", color = m.muted, fontSize = 15.sp)
                        inner()
                    }
                },
            )
            if (vm.searchTerm.isNotEmpty()) {
                Icon(Icons.Rounded.Close, "Clear search", tint = m.muted, modifier = Modifier.clickable { vm.searchTerm = "" })
            }
        }
    }
}

@Composable
private fun NavBar(vm: MiraViewModel) {
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

@Composable
private fun MusicScreen(vm: MiraViewModel, openPlayer: () -> Unit, goSettings: () -> Unit) {
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

@Composable
private fun PlayerBar(vm: MiraViewModel, onExpand: () -> Unit, onDevices: () -> Unit) {
    val m = Mira.colors
    val pb = vm.playback
    var now by remember { mutableLongStateOf(android.os.SystemClock.elapsedRealtime()) }
    LaunchedEffect(pb?.playing) { while (pb?.playing == true) { now = android.os.SystemClock.elapsedRealtime(); delay(200) } }
    Box(Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(20.dp)).background(m.elevated).clickable(onClick = onExpand)) {
        Row(Modifier.fillMaxSize().padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 26.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(pb?.track?.cover, 52.dp, radius = 14.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(pb?.track?.name ?: "Nothing playing", color = m.text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    pb?.track?.subtitle ?: if (vm.deviceId.isEmpty()) "Pick a song: Mira opens Spotify for you" else "Choose a song to start",
                    color = m.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (pb == null) RoundButton(Icons.Rounded.Devices, "Choose playback device", onDevices)
            else {
                RoundButton(if (pb.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (pb.playing) "Pause" else "Play", { vm.togglePlayback() }, filled = true, enabled = !pb.restricted)
                RoundButton(Icons.Rounded.SkipNext, "Next track", { vm.command("next") }, enabled = pb.allows("skipping_next"))
            }
        }
        WavyProgress(
            pb?.position(now) ?: 0, pb?.track?.durationMs ?: 0, pb?.playing == true, seekable = false, onSeek = {},
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 14.dp, vertical = 4.dp), height = 16.dp,
        )
    }
}

@Composable
private fun NowPlaying(vm: MiraViewModel, onClose: () -> Unit, onDevices: () -> Unit) {
    val m = Mira.colors
    val pb = vm.playback
    var now by remember { mutableLongStateOf(android.os.SystemClock.elapsedRealtime()) }
    LaunchedEffect(pb?.playing) { while (pb?.playing == true) { now = android.os.SystemClock.elapsedRealtime(); delay(100) } }
    Column(
        Modifier.fillMaxSize().background(m.base).windowInsetsPadding(WindowInsets.systemBars).padding(20.dp).clickable(enabled = false) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth()) { RoundButton(Icons.Rounded.KeyboardArrowDown, "Close", onClose, selected = true) }
        Spacer(Modifier.weight(1f))
        Cover(pb?.track?.cover, 300.dp, radius = 32.dp)
        Spacer(Modifier.height(24.dp))
        Text(pb?.track?.name ?: "Nothing playing", color = m.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(pb?.track?.subtitle ?: "Choose a song to start", color = m.muted, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        pb?.track?.album?.takeIf { it.isNotEmpty() }?.let { Text(it, color = m.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        Spacer(Modifier.height(16.dp))
        val position = pb?.position(now) ?: 0
        WavyProgress(position, pb?.track?.durationMs ?: 0, pb?.playing == true, seekable = pb?.allows("seeking") == true, onSeek = { vm.seek(it) }, height = 28.dp)
        Row(Modifier.fillMaxWidth()) {
            Text(formatTime(position), color = m.muted, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            Text(formatTime(pb?.track?.durationMs ?: 0), color = m.muted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundButton(Icons.Rounded.Shuffle, "Shuffle", { vm.setShuffle(pb?.shuffle != true) }, selected = pb?.shuffle == true, enabled = pb != null && pb.allows("toggling_shuffle"))
            RoundButton(Icons.Rounded.SkipPrevious, "Previous", { vm.command("previous") }, enabled = pb != null && pb.allows("skipping_prev"))
            RoundButton(if (pb?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play or pause", { vm.togglePlayback() }, filled = true, size = 68.dp, enabled = pb?.restricted != true)
            RoundButton(Icons.Rounded.SkipNext, "Next", { vm.command("next") }, enabled = pb != null && pb.allows("skipping_next"))
            RoundButton(if (pb?.repeat == "track") Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repeat", { vm.cycleRepeat() }, selected = pb != null && pb.repeat != "off", enabled = pb != null)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = m.muted)
            var volume by remember(pb?.volume) { mutableFloatStateOf((pb?.volume ?: 0).toFloat()) }
            Slider(volume, { volume = it }, onValueChangeFinished = { vm.setVolume(volume.toInt()) }, valueRange = 0f..100f, enabled = pb?.supportsVolume == true, modifier = Modifier.weight(1f))
            RoundButton(Icons.Rounded.Devices, "Devices", onDevices, size = 40.dp)
        }
        pb?.deviceName?.takeIf { it.isNotEmpty() }?.let { Text("Playing on $it", color = m.primary, fontSize = 12.sp) }
        Spacer(Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DevicesSheet(vm: MiraViewModel, onClose: () -> Unit) {
    val m = Mira.colors
    LaunchedEffect(Unit) { vm.refreshPlayer(true) }
    ModalBottomSheet(onDismissRequest = onClose, containerColor = m.elevated, contentColor = m.text) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Where do you want to listen?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = m.text)
            Button(onClick = { vm.openSpotifyApp(); onClose() }, enabled = vm.connected, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = m.primary, contentColor = m.onPrimary)) {
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, null); Spacer(Modifier.width(8.dp)); Text("Open Spotify on this phone")
            }
            if (vm.devices.isEmpty()) Text("No devices found. Open Spotify on this phone or another device.", color = m.muted)
            vm.devices.forEach { d ->
                val selected = d.id == (vm.playback?.deviceId?.takeIf { it.isNotEmpty() } ?: vm.deviceId)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (selected) m.container else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { vm.selectDevice(d.id); onClose() }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(if (d.type.equals("Smartphone", true)) Icons.Rounded.PhoneAndroid else Icons.Rounded.Speaker, null, tint = if (selected) m.primary else m.muted)
                    Spacer(Modifier.width(12.dp))
                    Text(d.name, color = m.text, modifier = Modifier.weight(1f))
                    if (selected) Icon(Icons.Rounded.Check, null, tint = m.primary)
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(vm: MiraViewModel) {
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
