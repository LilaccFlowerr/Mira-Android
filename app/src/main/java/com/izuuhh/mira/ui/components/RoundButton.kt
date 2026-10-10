package com.izuuhh.mira.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.izuuhh.mira.ui.theme.Mira

@Composable
fun RoundButton(
    icon: ImageVector, description: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, filled: Boolean = false, selected: Boolean = false, enabled: Boolean = true, size: Dp = 48.dp,
) {
    val m = Mira.colors
    IconButton(
        onClick = onClick, enabled = enabled, modifier = modifier.size(size),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = when { filled -> m.primary; selected -> m.container; else -> androidx.compose.ui.graphics.Color.Transparent },
            contentColor = when { filled -> m.onPrimary; selected -> m.primary; else -> m.text },
            disabledContentColor = m.muted.copy(alpha = 0.5f),
            disabledContainerColor = if (filled) m.hover else androidx.compose.ui.graphics.Color.Transparent,
        ),
    ) { Icon(icon, description) }
}

/** Progress line that is wavy for the played part while music plays; tap or drag to seek. */
