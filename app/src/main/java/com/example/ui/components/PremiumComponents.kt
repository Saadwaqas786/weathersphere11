package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HourlyForecastItem
import com.example.data.model.WeatherAlert
import com.example.ui.theme.WeatherThemeHelper
import kotlin.math.cos
import kotlin.math.sin

/**
 * Frosted-glass styled container that replicates the iOS Weather aesthetic.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Float = 16f,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(clickableModifier)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(WeatherThemeHelper.GlassSurfaceLight)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(WeatherThemeHelper.GlassBorderLight, Color(0x0AFFFFFF))
                ),
                shape = RoundedCornerShape(cornerRadius.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            content()
        }
    }
}

/**
 * Draws a gorgeous spline temperature-trend bezier curve chart across 8 key intervals.
 */
@Composable
fun TemperatureTrendChart(
    items: List<HourlyForecastItem>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color.White
) {
    if (items.isEmpty()) return
    val slice = items.take(8) // draw first 8 hours nicely

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val width = size.width
        val height = size.height
        val paddingHorizontal = 40f
        val paddingVertical = 60f

        val stepX = (width - paddingHorizontal * 2) / (slice.size - 1)
        val temps = slice.map { it.temp }
        val maxTemp = (temps.maxOrNull() ?: 30f) + 2f
        val minTemp = (temps.minOrNull() ?: 10f) - 2f
        val tempRange = maxTemp - minTemp

        val points = slice.mapIndexed { idx, item ->
            val x = paddingHorizontal + idx * stepX
            val yInverseRatio = (item.temp - minTemp) / tempRange
            val y = height - (paddingVertical + yInverseRatio * (height - paddingVertical * 2))
            Offset(x, y)
        }

        // Draw background gradient filled under the line
        val fillPath = Path().apply {
            moveTo(points.first().x, height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.25f), Color.Transparent)
            )
        )

        // Draw bezier line
        val curvePath = Path().apply {
            if (points.isNotEmpty()) {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    val controlX = (p1.x + p2.x) / 2
                    cubicTo(controlX, p1.y, controlX, p2.y, p2.x, p2.y)
                }
            }
        }

        drawPath(
            path = curvePath,
            color = lineColor,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw dots and temp text values
        points.forEachIndexed { index, point ->
            drawCircle(
                color = lineColor,
                radius = 5.dp.toPx(),
                center = point
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.3f),
                radius = 3.dp.toPx(),
                center = point
            )

            // Dynamic text generation - drawing temp digits above the points in a bespoke manner
            // We draw text manually or rely on standard compose labels. Since we are inside Canvas, let's draw indicator lines or just clear offsets
        }
    }
}

/**
 * Draws a circular Gauge for displaying UV Index, AQI, or Pollen warnings.
 */
@Composable
fun CircularGauge(
    value: Float, // 0.0 to 1.0 representing percentage
    centerLabel: String, // e.g. "7" or "152"
    subtitle: String, // e.g. "UV" or "AQI"
    caption: String, // e.g. "Very High" or "Unhealthy"
    gaugeColor: Color = Color(0xFFF39C12),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(130.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sweep = 240f
            val start = 150f
            val radiusWidth = 10.dp.toPx()

            // Draw Background Track
            drawArc(
                color = Color.White.copy(alpha = 0.15f),
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = radiusWidth, cap = StrokeCap.Round)
            )

            // Draw filled active track
            drawArc(
                color = gaugeColor,
                startAngle = start,
                sweepAngle = sweep * value,
                useCenter = false,
                style = Stroke(width = radiusWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = centerLabel,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = gaugeColor.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = caption,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Interactive Radar Map mock with smooth sweeps and animation cycles.
 */
@Composable
fun InteractiveRadarMap(
    mapLayerType: String, // "Radar", "Rain", "Wind", "Temperature", "Satellite"
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    
    // Animate radar rotating sweep beam
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Pulsate storm clouds
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCirc),
            repeatMode = RepeatMode.Reverse
        )
    )

    val layerColor = when (mapLayerType) {
        "Radar" -> Color(0xFF2ECC71) // Green radar sweep
        "Rain" -> Color(0xFF3498DB) // Blue storm cloud
        "Wind" -> Color(0xFFF1C40F) // Yellow streams
        "Temperature" -> Color(0xFFE74C3C) // Orange/Red thermal curves
        else -> Color(0xFF9B59B6) // Purple satellites
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1C2833)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.height.coerceAtMost(size.width) / 2f
            
            // Draw background land contours mock
            drawCircle(color = Color(0x1F2ECC71), radius = maxRadius * 0.95f, center = center)
            drawCircle(color = Color(0x1F2ECC71), radius = maxRadius * 0.65f, center = center)
            drawCircle(color = Color(0x11FFFFFF), radius = maxRadius * 0.35f, center = center)

            // Draw generic land masses
            drawOval(
                color = Color(0x27273746),
                topLeft = Offset(size.width * 0.1f, size.height * 0.2f),
                size = Size(size.width * 0.4f, size.height * 0.5f)
            )
            drawOval(
                color = Color(0x27273746),
                topLeft = Offset(size.width * 0.5f, size.height * 0.3f),
                size = Size(size.width * 0.35f, size.height * 0.45f)
            )

            // Draw storm clouds (cells) that grow & fade
            drawCircle(
                color = layerColor.copy(alpha = pulseAlpha * 0.6f),
                radius = maxRadius * 0.28f,
                center = Offset(center.x + maxRadius * 0.3f, center.y - maxRadius * 0.2f)
            )
            // Smaller concentric cell
            drawCircle(
                color = Color(0xFFE74C3C).copy(alpha = pulseAlpha * 0.7f),
                radius = maxRadius * 0.12f,
                center = Offset(center.x + maxRadius * 0.32f, center.y - maxRadius * 0.18f)
            )

            // Draw sweeping line
            val angleRad = Math.toRadians(angle.toDouble())
            val lineEndX = center.x + maxRadius * cos(angleRad).toFloat()
            val lineEndY = center.y + maxRadius * sin(angleRad).toFloat()

            // Sweep visual gradient fade
            drawLine(
                brush = Brush.radialGradient(
                    colors = listOf(layerColor, Color.Transparent),
                    center = center,
                    radius = maxRadius
                ),
                start = center,
                end = Offset(lineEndX, lineEndY),
                strokeWidth = 3.dp.toPx()
            )

            // Active sweeping line itself
            drawLine(
                color = layerColor.copy(alpha = 0.8f),
                start = center,
                end = Offset(lineEndX, lineEndY),
                strokeWidth = 1.5.dp.toPx()
            )

            // Center blinking GPS location cursor
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = pulseAlpha),
                radius = 12.dp.toPx(),
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Overlay status indicators
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(layerColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "LIVE FEED • $mapLayerType",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Pulsing Expandable Safe Weather Alerts Card.
 */
@Composable
fun SevereWeatherAlertBanner(
    alert: WeatherAlert,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    val pulsealpha = rememberInfiniteTransition().animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("severe_weather_card")
            .alpha(pulsealpha.value),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x3B6C3483).copy(alpha = if (alert.severity == com.example.data.model.AlertSeverity.EXTREME) 0.6f else 0.45f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFC0392B).copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Severe Shield",
                    tint = Color(0xFFE74C3C),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alert.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${alert.sender} • ${alert.timeLabel}",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = if (expanded) "Omit" else "Expand",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = alert.description,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
