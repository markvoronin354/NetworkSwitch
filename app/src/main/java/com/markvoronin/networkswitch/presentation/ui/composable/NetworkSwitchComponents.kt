package com.markvoronin.networkswitch.presentation.ui.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularOff
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.markvoronin.networkswitch.R
import com.markvoronin.networkswitch.domain.model.CompatibilityState
import com.markvoronin.networkswitch.domain.model.ControlMethod
import com.markvoronin.networkswitch.domain.model.NetworkMode
import com.markvoronin.networkswitch.domain.model.NetworkStats
import com.markvoronin.networkswitch.presentation.ui.components.SectionHeader
import com.markvoronin.networkswitch.presentation.ui.components.ShapeBadge
import com.markvoronin.networkswitch.presentation.ui.components.segmentedItemColors
import com.markvoronin.networkswitch.util.Utils

private fun ControlMethod.displayName() = if (this == ControlMethod.SHIZUKU) "Shizuku" else "Root"

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CompatibilityCard(
    compatibilityState: CompatibilityState,
    currentControlMethod: ControlMethod,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = compatibilityState,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "CompatibilityStateAnimation",
        modifier = modifier.fillMaxWidth()
    ) { state ->
        when (state) {
            is CompatibilityState.Pending -> StatusLayout(
                isError = false,
                title = stringResource(R.string.checking_compatibility),
                leading = { LoadingIndicator(modifier = Modifier.size(40.dp)) }
            )

            is CompatibilityState.Compatible -> StatusLayout(
                isError = false,
                title = stringResource(R.string.device_compatible),
                body = stringResource(R.string.active_method, currentControlMethod.displayName()),
                leading = {
                    ShapeBadge(shape = MaterialShapes.Cookie9Sided.toShape()) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.compatible)
                        )
                    }
                }
            )

            is CompatibilityState.PermissionDenied -> StatusLayout(
                isError = true,
                title = stringResource(R.string.access_required, state.method.displayName()),
                body = stringResource(
                    if (state.method == ControlMethod.ROOT) R.string.grant_root_permission
                    else R.string.grant_shizuku_permission
                ),
                leading = {
                    ErrorBadge {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = stringResource(R.string.permission_denied)
                        )
                    }
                },
                actionText = stringResource(R.string.retry_permission_check),
                onActionClick = onRetryClick
            )

            is CompatibilityState.Incompatible -> StatusLayout(
                isError = true,
                title = stringResource(R.string.device_incompatible),
                body = state.reason,
                leading = {
                    ErrorBadge {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = stringResource(R.string.incompatible)
                        )
                    }
                },
                actionText = stringResource(R.string.retry),
                onActionClick = onRetryClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ErrorBadge(content: @Composable BoxScope.() -> Unit) {
    ShapeBadge(
        shape = MaterialShapes.SoftBurst.toShape(),
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError,
        content = content
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatusLayout(
    isError: Boolean,
    title: String,
    leading: @Composable () -> Unit,
    body: String? = null,
    actionText: String? = null,
    onActionClick: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (isError) colors.errorContainer else colors.surfaceContainer,
        contentColor = if (isError) colors.onErrorContainer else colors.onSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                leading()
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (body != null) {
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isError) colors.onErrorContainer else colors.onSurfaceVariant
                        )
                    }
                }
            }
            if (actionText != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onActionClick,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        contentColor = colors.onError
                    ),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(actionText)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NetworkToggleCard(
    currentMode: NetworkMode?,
    toggleButtonText: String,
    isLoading: Boolean,
    onToggleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonHeight = ButtonDefaults.MediumContainerHeight

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(title = stringResource(R.string.network_mode))

        // Hero display of current network mode
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // The mode badge turns into the morphing loading indicator while switching
                    AnimatedContent(
                        targetState = isLoading,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "ModeBadgeAnimation"
                    ) { loading ->
                        if (loading) {
                            LoadingIndicator(
                                modifier = Modifier.size(88.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            ShapeBadge(
                                shape = MaterialShapes.Cookie9Sided.toShape(),
                                size = 88.dp,
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                if (currentMode != null) {
                                    Text(
                                        text = currentMode.tileLabel,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.SignalCellularOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.current_mode),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentMode?.displayName ?: stringResource(R.string.unavailable),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onToggleClick,
                    shapes = ButtonDefaults.shapes(),
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = buttonHeight),
                    contentPadding = ButtonDefaults.contentPaddingFor(buttonHeight)
                ) {
                    if (!isLoading) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.iconSizeFor(buttonHeight))
                        )
                        Spacer(modifier = Modifier.width(ButtonDefaults.iconSpacingFor(buttonHeight)))
                    }
                    Text(
                        text = if (isLoading) stringResource(R.string.switching) else toggleButtonText,
                        style = ButtonDefaults.textStyleFor(buttonHeight)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NetworkStatsCard(
    stats: NetworkStats,
    speedTestResults: Map<NetworkMode, Long>,
    isSpeedTestRunning: Boolean,
    canRunSpeedTest: Boolean,
    onRunSpeedTestClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(
            title = stringResource(R.string.network_performance),
            description = stringResource(R.string.network_performance_subtitle)
        )

        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            StatItem(
                label = stringResource(R.string.download),
                value = Utils.formatSpeed(stats.downloadBps),
                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 3)
            ) {
                Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null)
            }
            StatItem(
                label = stringResource(R.string.upload),
                value = Utils.formatSpeed(stats.uploadBps),
                shapes = ListItemDefaults.segmentedShapes(index = 1, count = 3)
            ) {
                Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = null)
            }
            StatItem(
                label = stringResource(R.string.signal_strength),
                value = stats.signalDbm?.let { stringResource(R.string.signal_dbm, it) }
                    ?: stringResource(R.string.unavailable),
                shapes = ListItemDefaults.segmentedShapes(index = 2, count = 3)
            ) {
                Icon(imageVector = Icons.Default.SignalCellularAlt, contentDescription = null)
            }
        }

        if (speedTestResults.isNotEmpty()) {
            SectionHeader(title = stringResource(R.string.speed_test_results))
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                speedTestResults.entries.forEachIndexed { index, (mode, bitsPerSecond) ->
                    StatItem(
                        label = mode.displayName,
                        value = Utils.formatSpeed(bitsPerSecond),
                        shapes = ListItemDefaults.segmentedShapes(index = index, count = speedTestResults.size)
                    ) {
                        Text(
                            text = mode.tileLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        val buttonHeight = ButtonDefaults.MediumContainerHeight
        FilledTonalButton(
            onClick = onRunSpeedTestClick,
            shapes = ButtonDefaults.shapes(),
            enabled = canRunSpeedTest && !isSpeedTestRunning,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = buttonHeight),
            contentPadding = ButtonDefaults.contentPaddingFor(buttonHeight)
        ) {
            if (isSpeedTestRunning) {
                LoadingIndicator(
                    modifier = Modifier.size(ButtonDefaults.iconSizeFor(buttonHeight)),
                    color = LocalContentColor.current
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.iconSpacingFor(buttonHeight)))
            }
            Text(
                text = stringResource(
                    if (isSpeedTestRunning) R.string.speed_test_running else R.string.run_speed_test
                ),
                style = ButtonDefaults.textStyleFor(buttonHeight)
            )
        }

        Text(
            text = stringResource(R.string.speed_test_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatItem(
    label: String,
    value: String,
    shapes: ListItemShapes,
    badge: @Composable BoxScope.() -> Unit
) {
    SegmentedListItem(
        shapes = shapes,
        colors = segmentedItemColors(),
        leadingContent = {
            ShapeBadge(shape = MaterialShapes.Cookie9Sided.toShape(), content = badge)
        },
        trailingContent = {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        content = { Text(label) }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QuickSettingsHintCard(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    SegmentedListItem(
        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
        colors = ListItemDefaults.segmentedColors(
            containerColor = colors.tertiaryContainer,
            contentColor = colors.onTertiaryContainer,
            supportingContentColor = colors.onTertiaryContainer
        ),
        modifier = modifier,
        leadingContent = {
            ShapeBadge(
                shape = MaterialShapes.Sunny.toShape(),
                containerColor = colors.tertiary,
                contentColor = colors.onTertiary
            ) {
                Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null)
            }
        },
        supportingContent = { Text(stringResource(R.string.qs_tile_hint)) },
        content = {
            Text(
                text = stringResource(R.string.qs_tile_title),
                fontWeight = FontWeight.Bold
            )
        }
    )
}
