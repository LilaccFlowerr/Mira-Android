package com.izuuhh.mira.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.izuuhh.mira.ui.theme.Mira
import com.izuuhh.mira.ui.theme.corner

@Composable
fun Cover(url: String?, size: Dp, modifier: Modifier = Modifier, radius: Dp = 12.dp, description: String? = null) {
    val m = Mira.colors
    Box(modifier.size(size).clip(RoundedCornerShape(corner(radius))).background(m.container)) {
        if (url != null) AsyncImage(url, description, Modifier.size(size), contentScale = ContentScale.Crop)
    }
}

/** Round icon button; [filled] gives the primary pill used for play/pause. */
