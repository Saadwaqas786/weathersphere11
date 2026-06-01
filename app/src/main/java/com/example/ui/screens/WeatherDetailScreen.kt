package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.WeatherCondition
import com.example.data.model.WeatherData
import com.example.ui.WeatherViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.TemperatureTrendChart

@Composable
fun WeatherDetailScreen(
    viewModel: WeatherViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val weatherData by viewModel.selectedWeatherData.collectAsStateWithLifecycle()
    var isShareReportDialogVisible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header row with back icon ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Go back", tint = Color.White)
                }
                
                weatherData?.cityName?.let { name ->
                    Text(
                        text = "$name Forecasts",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = { isShareReportDialogVisible = true }) {
                    Icon(Icons.Default.Share, contentDescription = "Share meteorological report", tint = Color.White)
                }
            }
        }

        weatherData?.let { data ->
            // --- Bezier spline temperature trend chart ---
            item {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TEMPERATURE TREND • NEXT 8 Hours",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TemperatureTrendChart(
                        items = data.hourlyForecast,
                        lineColor = Color.White
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Steady curve. Expected highs peak around afternoon with sweet thermal dissipation at sunset.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // --- Hourly timeline 48 hours list ---
            item {
                Column {
                    Text(
                        text = "48-HOUR DETAILED TIMELINE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(data.hourlyForecast) { hourly ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier.width(75.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(hourly.time, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    val icon = when (hourly.condition) {
                                        WeatherCondition.SUNNY -> Icons.Default.WbSunny
                                        WeatherCondition.CLOUDY -> Icons.Default.Cloud
                                        WeatherCondition.RAINY -> Icons.Default.WaterDrop
                                        WeatherCondition.SNOW -> Icons.Default.AcUnit
                                        WeatherCondition.THUNDERSTORM -> Icons.Default.FlashOn
                                        WeatherCondition.FOG -> Icons.Default.BlurOn
                                        WeatherCondition.NIGHT -> Icons.Default.Nightlight
                                    }
                                    
                                    val iconColor = when (hourly.condition) {
                                        WeatherCondition.SUNNY -> Color(0xFFF1C40F)
                                        WeatherCondition.THUNDERSTORM -> Color(0xFFF39C12)
                                        WeatherCondition.RAINY -> Color(0xFF3498DB)
                                        else -> Color.White
                                    }

                                    Icon(icon, contentDescription = hourly.condition.name, tint = iconColor, modifier = Modifier.size(24.dp))
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${hourly.temp.toInt()}°C", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    
                                    if (hourly.rainProbability > 0) {
                                        Text("${hourly.rainProbability}% 💧", fontSize = 9.sp, color = Color(0xFF5DADE2), fontWeight = FontWeight.Bold)
                                    } else {
                                        Text("0% ❄️", fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- 14-Day prognosis list ---
            item {
                Column {
                    Text(
                        text = "14-DAY ATMOSPHERIC OUTLOOK SCHEDULE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    GlassCard {
                        data.dailyForecast.forEachIndexed { index, daily ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("daily_forecast_item_${index}")
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.width(80.dp)) {
                                    Text(
                                        text = daily.dayName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = daily.dateLabel,
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                }

                                val icon = when (daily.condition) {
                                    WeatherCondition.SUNNY -> Icons.Default.WbSunny
                                    WeatherCondition.CLOUDY -> Icons.Default.Cloud
                                    WeatherCondition.RAINY -> Icons.Default.WaterDrop
                                    WeatherCondition.SNOW -> Icons.Default.AcUnit
                                    WeatherCondition.THUNDERSTORM -> Icons.Default.FlashOn
                                    WeatherCondition.FOG -> Icons.Default.BlurOn
                                    WeatherCondition.NIGHT -> Icons.Default.Nightlight
                                }
                                val iconColor = when (daily.condition) {
                                    WeatherCondition.SUNNY -> Color(0xFFF1C40F)
                                    WeatherCondition.THUNDERSTORM -> Color(0xFFF39C12)
                                    WeatherCondition.RAINY -> Color(0xFF3498DB)
                                    else -> Color.White
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(60.dp)) {
                                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    if (daily.rainProbability > 10) {
                                        Text("${daily.rainProbability}%", color = Color(0xFF5DADE2), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text("Dry", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                                    }
                                }

                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${daily.tempMin.toInt()}°", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                                    
                                    // Visual color slider representation for temperatures range
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(70.dp)
                                            .height(4.dp)
                                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(0.6f)
                                                .align(Alignment.Center)
                                                .background(Color(0xFFF39C12), RoundedCornerShape(2.dp))
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text("${daily.tempMax.toInt()}°", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (index < data.dailyForecast.size - 1) {
                                Divider(color = Color.White.copy(alpha = 0.08f))
                            }
                        }
                    }
                }
            }

            // --- Extra Secondary Grid (Visibility, Pressure, Dew Point) ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pressure Widget
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Compress, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("BAROMETRIC PRESSURE", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("${data.pressure} hPa", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text("Atmosphere index is perfectly stable.", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }

                    // Visibility Widget
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("VISIBILITY RANGE", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("${String.format("%.1f", data.visibility)} km", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text(
                                text = if (data.condition == WeatherCondition.FOG) "Dense cloud mist active." else "Skies are perfectly clear.",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Dew Point Widget
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DeviceThermostat, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("DEW POINT LEVEL", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("${String.format("%.1f", data.dewPoint)}°C", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text("Dew condensation is balanced.", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }

                    // Global location coordinates card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Map, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SATELLITE POSITION", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Coordinates", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Lat: ${String.format("%.3f", data.latitude)}", fontSize = 11.sp, color = Color.White)
                            Text("Lon: ${String.format("%.3f", data.longitude)}", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }

            // Share meteorological forecast text compilation dialog
            item {
                if (isShareReportDialogVisible) {
                    AlertDialog(
                        onDismissRequest = { isShareReportDialogVisible = false },
                        title = { Text("Share WeatherSphere Report", color = Color.White, fontWeight = FontWeight.Bold) },
                        containerColor = Color(0xFF1C2833),
                        text = {
                            val report = """
                                *WEATHERSPHERE METEOROLOGICAL FORECAST* 🌍
                                City: ${data.cityName}, ${data.country}
                                Current Temp: ${data.temp.toInt()}°C (Feels like: ${data.feelsLike.toInt()}°C)
                                Skies outlook: ${data.conditionDescription}
                                Winds: ${data.windSpeed.toInt()} km/h blowing ${data.windDirection}
                                Humidity: ${data.humidity}% | UV Index: ${data.uvIndex}
                                Air Quality index: ${data.aqi} (${data.aqiLabel})
                                
                                Weekly insights powered by WeatherSphere AI. Have a wholesome day! ✨
                            """.trimIndent()
                            
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Below report text is compiled to share to your calendar or chat apps:", color = Color.LightGray, fontSize = 12.sp)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(report, color = Color.White, fontSize = 11.sp)
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "Meteorological report for ${data.cityName}: ${data.temp.toInt()}C under ${data.conditionDescription.lowercase()} skies. Powered by WeatherSphere!")
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                    isShareReportDialogVisible = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E86C1))
                            ) {
                                Text("Send to Apps", color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { isShareReportDialogVisible = false }) {
                                Text("Omit", color = Color.White)
                            }
                        }
                    )
                }
            }
        }
    }
}
