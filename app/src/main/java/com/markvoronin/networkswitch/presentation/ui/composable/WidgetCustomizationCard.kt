package com.markvoronin.networkswitch.presentation.ui.composable

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markvoronin.networkswitch.R
import com.markvoronin.networkswitch.domain.model.WidgetCustomizationConfig
import com.markvoronin.networkswitch.presentation.ui.components.SectionHeader
import com.markvoronin.networkswitch.presentation.ui.components.ShapeBadge
import com.markvoronin.networkswitch.util.WidgetThemeHelper
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WidgetCustomizationCard(
    config: WidgetCustomizationConfig,
    onConfigChanged: (WidgetCustomizationConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    // Drags update these drafts (and the live preview) only; the config is saved,
    // and the real widgets re-rendered, once when the gesture ends.
    var draftColor by remember(config.customColorHex) { mutableIntStateOf(config.customColorHex) }
    var draftOpacity by remember(config.opacity) { mutableFloatStateOf(config.opacity) }
    val previewConfig = config.copy(customColorHex = draftColor, opacity = draftOpacity)

    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    // Preview, system color toggle, custom color, opacity. The custom color item is always a
    // middle item, so every item's shape stays the same when it is hidden.
    val itemCount = 4

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(
            title = stringResource(R.string.widget_customization),
            description = stringResource(R.string.widget_customization_desc)
        )

        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            // Live Widget Preview
            Surface(
                shape = ListItemDefaults.segmentedShapes(index = 0, count = itemCount).shape,
                color = colors.surfaceContainer
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    WidgetPreviewBox(config = previewConfig)
                }
            }

            // System Color Toggle. The switch shows the state, so the item keeps its
            // unselected look when checked.
            val toggleShapes = ListItemDefaults.segmentedShapes(index = 1, count = itemCount)
            SegmentedListItem(
                checked = config.useSystemColor,
                onCheckedChange = { useSystem ->
                    onConfigChanged(config.copy(useSystemColor = useSystem))
                },
                shapes = toggleShapes.copy(selectedShape = toggleShapes.shape),
                colors = ListItemDefaults.segmentedColors(
                    containerColor = colors.surfaceContainer,
                    selectedContainerColor = colors.surfaceContainer,
                    selectedContentColor = colors.onSurface,
                    selectedSupportingContentColor = colors.onSurfaceVariant
                ),
                leadingContent = {
                    ShapeBadge(shape = MaterialShapes.Cookie9Sided.toShape()) {
                        Icon(imageVector = Icons.Default.Contrast, contentDescription = null)
                    }
                },
                trailingContent = {
                    Switch(
                        checked = config.useSystemColor,
                        onCheckedChange = null,
                        thumbContent = if (config.useSystemColor) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        } else {
                            null
                        }
                    )
                },
                supportingContent = { Text(stringResource(R.string.match_system_theme_desc)) },
                content = {
                    Text(
                        text = stringResource(R.string.match_system_theme),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )

            // Custom Color Controls
            AnimatedVisibility(
                visible = !config.useSystemColor,
                enter = expandVertically(motion.defaultSpatialSpec()) + fadeIn(motion.defaultEffectsSpec()),
                exit = shrinkVertically(motion.defaultSpatialSpec()) + fadeOut(motion.defaultEffectsSpec())
            ) {
                Surface(
                    shape = ListItemDefaults.segmentedShapes(index = 2, count = itemCount).shape,
                    color = colors.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.custom_widget_color),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )

                        // Color Wheel
                        ColorWheelPicker(
                            currentColor = Color(draftColor),
                            onColorSelected = { selectedColor -> draftColor = selectedColor.toArgb() },
                            onColorSelectionFinished = {
                                onConfigChanged(config.copy(customColorHex = draftColor))
                            }
                        )

                        // Color Palette Presets
                        ColorPresets(
                            selectedColorHex = draftColor,
                            onColorSelected = { hex ->
                                draftColor = hex
                                onConfigChanged(config.copy(customColorHex = hex))
                            }
                        )
                    }
                }
            }

            // Opacity Slider
            Surface(
                shape = ListItemDefaults.segmentedShapes(index = 3, count = itemCount).shape,
                color = colors.surfaceContainer
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.opacity),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )
                        val percent = (draftOpacity * 100).toInt()
                        val transPercent = 100 - percent
                        Surface(
                            shape = CircleShape,
                            color = colors.primaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.opacity_value, percent, transPercent),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Slider(
                        value = draftOpacity,
                        onValueChange = { newOpacity -> draftOpacity = newOpacity },
                        onValueChangeFinished = { onConfigChanged(config.copy(opacity = draftOpacity)) },
                        valueRange = 0.0f..1.0f
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetPreviewBox(config: WidgetCustomizationConfig) {
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()
    val isDarkPreview = if (config.useSystemColor) {
        systemInDark
    } else {
        val red = (config.customColorHex shr 16) and 0xFF
        val green = (config.customColorHex shr 8) and 0xFF
        val blue = config.customColorHex and 0xFF
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0
        luminance < 0.6 || config.opacity < 0.4f
    }

    val baseColorInt = if (config.useSystemColor) {
        if (isDarkPreview) {
            WidgetThemeHelper.getSystemDarkColor(context)
        } else {
            WidgetThemeHelper.getSystemLightColor(context)
        }
    } else {
        config.customColorHex
    }

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val previewColorInt = (baseColorInt and 0x00FFFFFF) or (alphaInt shl 24)

    val previewColor = Color(previewColorInt)

    val textColor = if (isDarkPreview) Color.White else Color(0xFF1E293B)
    val subtitleColor = if (isDarkPreview) Color(0xFFCBD5E1) else Color(0xFF64748B)

    // Sleek Desktop/Wallpaper preview background
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF202938),
                        Color(0xFF161C26),
                        Color(0xFF0F131A)
                    )
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Widget Tile
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(previewColor)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge Icon with Network Switch Logo
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDarkPreview) Color(0xFF334155) else Color(0xFFE2E8F0)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                        contentDescription = stringResource(R.string.logo_description),
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.widget_default_title),
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = stringResource(R.string.widget_default_subtitle),
                        color = subtitleColor,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorWheelPicker(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onColorSelectionFinished: () -> Unit
) {
    val currentOnColorSelected by rememberUpdatedState(onColorSelected)
    val currentOnSelectionFinished by rememberUpdatedState(onColorSelectionFinished)

    // State for HSV
    var currentHue by remember { mutableFloatStateOf(0f) }
    var currentSat by remember { mutableFloatStateOf(0f) }
    var currentVal by remember { mutableFloatStateOf(1f) }

    val recentEmittedArgbs = remember { mutableSetOf<Int>() }

    LaunchedEffect(currentColor) {
        val targetArgb = currentColor.toArgb()
        if (!recentEmittedArgbs.contains(targetArgb)) {
            val hsvArray = FloatArray(3)
            AndroidColor.colorToHSV(targetArgb, hsvArray)
            currentHue = hsvArray[0]
            currentSat = hsvArray[1]
            currentVal = hsvArray[2]
            recentEmittedArgbs.clear()
            recentEmittedArgbs.add(targetArgb)
        }
    }

    fun emitColor(h: Float, s: Float, v: Float) {
        currentHue = h
        currentSat = s
        currentVal = v
        val argb = AndroidColor.HSVToColor(floatArrayOf(h, s, v))
        recentEmittedArgbs.add(argb)
        if (recentEmittedArgbs.size > 200) {
            recentEmittedArgbs.clear()
            recentEmittedArgbs.add(argb)
        }
        currentOnColorSelected(Color(argb))
    }

    val wheelSizeDp = 180.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brightness Slider on the LEFT
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.brightness),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            VerticalBrightnessSlider(
                height = wheelSizeDp,
                hue = currentHue,
                sat = currentSat,
                valValue = currentVal,
                onValChanged = { newVal ->
                    emitColor(currentHue, currentSat, newVal)
                },
                onValChangeFinished = { currentOnSelectionFinished() }
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Color Wheel on the RIGHT
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.hue_saturation),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(wheelSizeDp)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val (hue, sat) = hueAndSaturationAt(offset, size)
                                emitColor(hue, sat, currentVal)
                                currentOnSelectionFinished()
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = { currentOnSelectionFinished() },
                                onDragCancel = { currentOnSelectionFinished() }
                            ) { change, _ ->
                                change.consume()
                                val (hue, sat) = hueAndSaturationAt(change.position, size)
                                emitColor(hue, sat, currentVal)
                            }
                        }
                ) {
                    val radius = size.width / 2f
                    val center = Offset(radius, radius)

                    // Sweep gradient for Hue
                    val sweepColors = listOf(
                        Color.Red, Color.Yellow, Color.Green,
                        Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                    )
                    drawCircle(
                        brush = Brush.sweepGradient(sweepColors, center),
                        radius = radius,
                        center = center
                    )

                    // Radial gradient for Saturation (White in center)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, Color.Transparent),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )

                    // Black overlay for Brightness/Value
                    if (currentVal < 1.0f) {
                        drawCircle(
                            color = Color.Black.copy(alpha = 1.0f - currentVal),
                            radius = radius,
                            center = center
                        )
                    }

                    // Draw selector thumb
                    val angleRad = Math.toRadians(currentHue.toDouble())
                    val thumbDistance = currentSat * radius
                    val thumbX = center.x + (thumbDistance * cos(angleRad)).toFloat()
                    val thumbY = center.y + (thumbDistance * sin(angleRad)).toFloat()

                    drawCircle(
                        color = Color.White,
                        radius = 12.dp.toPx(),
                        center = Offset(thumbX, thumbY)
                    )
                    drawCircle(
                        color = Color(AndroidColor.HSVToColor(floatArrayOf(currentHue, currentSat, currentVal))),
                        radius = 9.dp.toPx(),
                        center = Offset(thumbX, thumbY)
                    )
                }
            }
        }
    }
}

/** Maps a touch point on the color wheel to hue (degrees) and saturation (0..1). */
private fun hueAndSaturationAt(offset: Offset, size: IntSize): Pair<Float, Float> {
    val radius = size.width / 2f
    val dx = offset.x - radius
    val dy = offset.y - size.height / 2f

    var hue = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    if (hue < 0) hue += 360f

    val saturation = (sqrt(dx * dx + dy * dy) / radius).coerceIn(0f, 1f)
    return hue to saturation
}

@Composable
private fun VerticalBrightnessSlider(
    height: Dp,
    width: Dp = 26.dp,
    hue: Float,
    sat: Float,
    valValue: Float,
    onValChanged: (Float) -> Unit,
    onValChangeFinished: () -> Unit
) {
    val currentOnValChanged by rememberUpdatedState(onValChanged)
    val currentOnValChangeFinished by rememberUpdatedState(onValChangeFinished)

    val topColor = remember(hue, sat) {
        Color(AndroidColor.HSVToColor(floatArrayOf(hue, sat, 1.0f)))
    }

    Box(
        modifier = Modifier
            .height(height)
            .width(width)
            .clip(RoundedCornerShape(width / 2))
            .background(
                Brush.verticalGradient(
                    colors = listOf(topColor, Color.Black)
                )
            )
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    currentOnValChanged(brightnessAt(offset.y, size.height))
                    currentOnValChangeFinished()
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { currentOnValChangeFinished() },
                    onDragCancel = { currentOnValChangeFinished() }
                ) { change, _ ->
                    change.consume()
                    currentOnValChanged(brightnessAt(change.position.y, size.height))
                }
            }
    ) {
        val thumbRadius = 11.dp

        Canvas(modifier = Modifier.fillMaxSize()) {
            val thumbY = ((1.0f - valValue) * size.height).coerceIn(thumbRadius.toPx(), size.height - thumbRadius.toPx())
            val centerX = size.width / 2f

            drawCircle(
                color = Color.White,
                radius = thumbRadius.toPx(),
                center = Offset(centerX, thumbY)
            )
            drawCircle(
                color = Color(AndroidColor.HSVToColor(floatArrayOf(hue, sat, valValue))),
                radius = (thumbRadius - 2.5.dp).toPx(),
                center = Offset(centerX, thumbY)
            )
        }
    }
}

/** Maps a vertical touch position to brightness (top = brightest), keeping a floor so the color never goes fully black. */
private fun brightnessAt(offsetY: Float, heightPx: Int): Float =
    (1.0f - offsetY.coerceIn(0f, heightPx.toFloat()) / heightPx).coerceIn(0.05f, 1.0f)

@Composable
private fun ColorPresets(
    selectedColorHex: Int,
    onColorSelected: (Int) -> Unit
) {
    // Muted, premium presets (No harsh intense neon or deep blue)
    val presets = remember {
        listOf(
            0xFF2D3B48.toInt(), // Slate Graphite
            0xFF181C22.toInt(), // Midnight Noir
            0xFF3A4750.toInt(), // Charcoal Grey
            0xFF2E5B60.toInt(), // Muted Teal
            0xFF3B5249.toInt(), // Muted Sage
            0xFF4A3B52.toInt(), // Deep Plum
            0xFF5C4A3E.toInt(), // Muted Amber
            0xFF3B485E.toInt()  // Cool Indigo Slate
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.preset_colors),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            presets.forEach { colorInt ->
                val isSelected = (selectedColorHex and 0x00FFFFFF) == (colorInt and 0x00FFFFFF)
                // Selected swatch springs from a circle into a squircle
                val cornerPercent by animateIntAsState(
                    targetValue = if (isSelected) 30 else 50,
                    animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                    label = "PresetCorner"
                )
                val shape = RoundedCornerShape(cornerPercent)

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(shape)
                        .background(Color(colorInt))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                            shape = shape
                        )
                        .clickable {
                            onColorSelected(colorInt)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
