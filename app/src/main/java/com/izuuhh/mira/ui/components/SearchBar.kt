package com.izuuhh.mira.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.izuuhh.mira.MiraViewModel
import com.izuuhh.mira.ui.theme.corner
import com.izuuhh.mira.ui.theme.Mira

@Composable
fun SearchBar(vm: MiraViewModel) {
    val m = Mira.colors
    val focus = LocalFocusManager.current
    Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("mira", color = m.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp, end = 12.dp))
        Row(
            Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(corner(22.dp))).background(m.elevated).padding(horizontal = 14.dp),
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
