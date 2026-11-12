package com.markvoronin.networkswitch.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Primary-colored section label (dot + uppercase title) shown above a group of items. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            action?.invoke()
        }
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, bottom = 4.dp)
            )
        }
    }
}

/** Icon or label inside an expressive shape, e.g. `MaterialShapes.Cookie9Sided.toShape()`. */
@Composable
fun ShapeBadge(
    shape: Shape,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier.size(size),
        shape = shape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Box(contentAlignment = Alignment.Center, content = content)
    }
}

/** Colors for [SegmentedListItem]s, which sit on the screen background in grouped lists. */
@Composable
fun segmentedItemColors(): ListItemColors =
    ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
