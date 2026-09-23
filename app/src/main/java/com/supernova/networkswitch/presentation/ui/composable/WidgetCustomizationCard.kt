package com.supernova.networkswitch.presentation.ui.composable

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supernova.networkswitch.R
import com.supernova.networkswitch.domain.model.WidgetCustomizationConfig
import com.supernova.networkswitch.util.WidgetThemeHelper
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun WidgetCustomizationCard(
    config: WidgetCustomizationConfig,
    onConfigChanged: (WidgetCustomizationConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column {
                Text(
                    text = "Widget Customization",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Personalize the appearance of your home screen widget",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Live Widget Preview
            WidgetPreviewBox(config = config)

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // System Color Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Match System Theme",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Automatically adapts widget color to dark or light system mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = config.useSystemColor,
                    onCheckedChange = { useSystem ->
                        onConfigChanged(config.copy(useSystemColor = useSystem))
                    }
                )
            }

            // Custom Color Controls
            AnimatedVisibility(
                visible = !config.useSystemColor,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Custom Widget Color",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Color Wheel
                    ColorWheelPicker(
                        currentColor = Color(config.customColorHex),
                        onColorSelected = { selectedColor ->
                            onConfigChanged(config.copy(customColorHex = selectedColor.toArgb()))
                        }
                    )

                    // Color Palette Presets
                    ColorPresets(
                        selectedColorHex = config.customColorHex,
                        onColorSelected = { hex ->
                            onConfigChanged(config.copy(customColorHex = hex))
                        }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Opacity Slider
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Opacity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val percent = (config.opacity * 100).toInt()
                    val transPercent = 100 - percent
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "$percent% ($transPercent% transparent)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Slider(
                    value = config.opacity,
                    onValueChange = { newOpacity ->
                        onConfigChanged(config.copy(opacity = newOpacity))
                    },
                    valueRange = 0.0f..1.0f
                )
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
                        contentDescription = "Network Switch Logo",
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "5G",
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Next: 4G",
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
    onColorSelected: (Color) -> Unit
) {
    val currentOnColorSelected by rememberUpdatedState(onColorSelected)

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
                text = "Brightness",
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
                }
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Color Wheel on the RIGHT
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Hue & Saturation",
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
                            fun processInput(offset: Offset) {
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                val dx = offset.x - centerX
                                val dy = offset.y - centerY
                                val radius = size.width / 2f

                                val distance = sqrt(dx * dx + dy * dy)
                                val clampedDistance = distance.coerceAtMost(radius)

                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < 0) angle += 360f

                                val sat = (clampedDistance / radius).coerceIn(0f, 1f)

                                emitColor(angle, sat, currentVal)
                            }

                            detectTapGestures { offset ->
                                processInput(offset)
                            }
                        }
                        .pointerInput(Unit) {
                            fun processInput(offset: Offset) {
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                val dx = offset.x - centerX
                                val dy = offset.y - centerY
                                val radius = size.width / 2f

                                val distance = sqrt(dx * dx + dy * dy)
                                val clampedDistance = distance.coerceAtMost(radius)

                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < 0) angle += 360f

                                val sat = (clampedDistance / radius).coerceIn(0f, 1f)

                                emitColor(angle, sat, currentVal)
                            }

                            detectDragGestures { change, _ ->
                                change.consume()
                                processInput(change.position)
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

@Composable
private fun VerticalBrightnessSlider(
    height: Dp,
    width: Dp = 26.dp,
    hue: Float,
    sat: Float,
    valValue: Float,
    onValChanged: (Float) -> Unit
) {
    val currentOnValChanged by rememberUpdatedState(onValChanged)

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
                fun processTouch(offsetY: Float) {
                    val clampedY = offsetY.coerceIn(0f, size.height.toFloat())
                    val newFraction = 1.0f - (clampedY / size.height.toFloat())
                    currentOnValChanged(newFraction.coerceIn(0.05f, 1.0f))
                }

                detectTapGestures { offset ->
                    processTouch(offset.y)
                }
            }
            .pointerInput(Unit) {
                fun processTouch(offsetY: Float) {
                    val clampedY = offsetY.coerceIn(0f, size.height.toFloat())
                    val newFraction = 1.0f - (clampedY / size.height.toFloat())
                    currentOnValChanged(newFraction.coerceIn(0.05f, 1.0f))
                }

                detectDragGestures { change, _ ->
                    change.consume()
                    processTouch(change.position.y)
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
            text = "Preset Colors",
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

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(colorInt))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .clickable {
                            onColorSelected(colorInt)
                        }
                )
            }
        }
    }
}
