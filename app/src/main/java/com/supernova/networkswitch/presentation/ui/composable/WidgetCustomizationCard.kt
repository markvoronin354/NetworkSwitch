package com.supernova.networkswitch.presentation.ui.composable

import android.R
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.SweepGradient
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supernova.networkswitch.domain.model.WidgetCustomizationConfig
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
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Widget Customization",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Customize the appearance of the home screen widget.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Live Widget Preview
            WidgetPreviewBox(config = config)

            // System Color Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Use System Color",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            "Use Material You dynamic color from system theme"
                        } else {
                            "Use default primary theme accent color"
                        },
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
            if (!config.useSystemColor) {
                Text(
                    text = "Widget Color",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
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

            HorizontalDivider()

            // Transparency / Opacity Slider
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Opacity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    val percent = (config.opacity * 100).toInt()
                    val transPercent = 100 - percent
                    Text(
                        text = "$percent% ($transPercent% transparent)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
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

    val systemAccentColor = remember {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getColor(R.color.system_accent1_600)
            } else {
                context.getColor(com.supernova.networkswitch.R.color.purple_700)
            }
        } catch (_: Exception) {
            AndroidColor.parseColor("#3700B3")
        }
    }

    val baseColorInt = if (config.useSystemColor) systemAccentColor else config.customColorHex
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val previewColorInt = (baseColorInt and 0x00FFFFFF) or (alphaInt shl 24)

    val previewColor = Color(previewColorInt)

    // Luminance check for text contrast
    val red = (baseColorInt shr 16) and 0xFF
    val green = (baseColorInt shr 8) and 0xFF
    val blue = baseColorInt and 0xFF
    val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0
    val isDark = luminance < 0.6 || config.opacity < 0.4f

    val textColor = if (isDark) Color.White else Color.Black
    val subtitleColor = if (isDark) Color(0xFFE0E0E0) else Color(0xFF424242)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Live Preview",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Wallpaper background box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1A237E),
                            Color(0xFF0D47A1),
                            Color(0xFF006064)
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
                    .clip(RoundedCornerShape(24.dp))
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
                                if (isDark) Color(0xFF4A4A4A) else Color(0xFF616161)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = com.supernova.networkswitch.R.mipmap.ic_launcher_foreground),
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

    // Ring buffer / set of recently emitted ARGB values to absorb asynchronous DataStore echo delays
    val recentEmittedArgbs = remember { mutableSetOf<Int>() }

    // Sync state when currentColor is changed from an external source (e.g. ColorPresets)
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
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brightness Slider on the LEFT
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
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

        Spacer(modifier = Modifier.width(20.dp))

        // Color Wheel on the RIGHT
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Color",
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
    width: Dp = 24.dp,
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
        val thumbRadius = 10.dp

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
    val presets = remember {
        listOf(
            0xFF333333.toInt(), // Default Grey
            0xFF121212.toInt(), // Dark Black
            0xFF455A64.toInt(), // Slate
            0xFF1976D2.toInt(), // Blue
            0xFF388E3C.toInt(), // Green
            0xFF00796B.toInt(), // Teal
            0xFF7B1FA2.toInt(), // Purple
            0xFFD32F2F.toInt(), // Red
            0xFFF57C00.toInt()  // Orange
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Preset Colors",
            style = MaterialTheme.typography.labelMedium,
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
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(colorInt))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
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
