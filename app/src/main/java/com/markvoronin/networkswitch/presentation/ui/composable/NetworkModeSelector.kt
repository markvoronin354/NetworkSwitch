package com.markvoronin.networkswitch.presentation.ui.composable

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.markvoronin.networkswitch.domain.model.NetworkMode
import com.markvoronin.networkswitch.presentation.ui.components.ShapeBadge
import com.markvoronin.networkswitch.presentation.ui.components.segmentedItemColors

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NetworkModeSelector(
    label: String,
    selectedMode: NetworkMode,
    onModeSelected: (NetworkMode) -> Unit,
    shapes: ListItemShapes,
    modifier: Modifier = Modifier,
    availableModes: List<NetworkMode> = NetworkMode.entries
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        SegmentedListItem(
            onClick = { expanded = true },
            shapes = shapes,
            colors = segmentedItemColors(),
            leadingContent = {
                ShapeBadge(shape = MaterialShapes.Cookie9Sided.toShape()) {
                    Text(
                        text = selectedMode.tileLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            trailingContent = {
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            },
            overlineContent = { Text(label) },
            content = {
                Text(
                    text = selectedMode.displayName,
                    fontWeight = FontWeight.SemiBold
                )
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.large
        ) {
            availableModes.forEach { mode ->
                val isSelected = mode == selectedMode
                DropdownMenuItem(
                    text = {
                        Text(
                            text = mode.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    trailingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        null
                    },
                    onClick = {
                        onModeSelected(mode)
                        expanded = false
                    }
                )
            }
        }
    }
}
