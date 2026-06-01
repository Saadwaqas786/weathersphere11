package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.WeatherViewModel
import com.example.ui.components.GlassCard

@Composable
fun WeatherComparisonScreen(
    viewModel: WeatherViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var cityAQuery by remember { mutableStateOf("New York") }
    var cityBQuery by remember { mutableStateOf("London") }

    val dataA by viewModel.compCityAData.collectAsStateWithLifecycle()
    val dataB by viewModel.compCityBData.collectAsStateWithLifecycle()

    // Query on first load
    LaunchedEffect(cityAQuery, cityBQuery) {
        viewModel.setComparisonCities(cityAQuery, cityBQuery)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header Section ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Go back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.Default.CompareArrows, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Inter-City Comparison Panel",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        // --- Cities Input Fields Row ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = cityAQuery,
                onValueChange = { cityAQuery = it },
                label = { Text("First City", color = Color.White.copy(alpha = 0.6f)) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = cityBQuery,
                onValueChange = { cityBQuery = it },
                label = { Text("Second City", color = Color.White.copy(alpha = 0.6f)) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )
        }

        // --- Side-by-Side Comparison Grid content ---
        if (dataA != null && dataB != null) {
            val a = dataA!!
            val b = dataB!!

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Titles
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E86C1).copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(a.cityName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            Text(a.country, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${a.temp.toInt()}°C", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                            Text(a.conditionDescription, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF8E44AD).copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(b.cityName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            Text(b.country, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${b.temp.toInt()}°C", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                            Text(b.conditionDescription, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Comparison Specs Specs Cards
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Comparison Parameters Spec Matrix", fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))

                    // Feels-like Comparison
                    ComparisonRowSpec(title = "Apparent Feels-Like", valA = "${a.feelsLike.toInt()}°C", valB = "${b.feelsLike.toInt()}°C")
                    Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

                    // Wind Comparison
                    ComparisonRowSpec(title = "Wind Vel & Direction", valA = "${a.windSpeed.toInt()} km/h (${a.windDirection})", valB = "${b.windSpeed.toInt()} km/h (${b.windDirection})")
                    Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

                    // Humidity Comparison
                    ComparisonRowSpec(title = "Moisture Humidity", valA = "${a.humidity}%", valB = "${b.humidity}%")
                    Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

                    // AQI Indicator Comparison
                    ComparisonRowSpec(title = "Air Quality Index (AQI)", valA = "${a.aqi} (${a.aqiLabel})", valB = "${b.aqi} (${b.aqiLabel})")
                    Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

                    // UV Index Comparison
                    ComparisonRowSpec(title = "Solar UV index", valA = a.uvIndex.toString(), valB = b.uvIndex.toString())
                    Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

                    // Barometric Pressure
                    ComparisonRowSpec(title = "Atmospheric Pressure", valA = "${a.pressure} hPa", valB = "${b.pressure} hPa")
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("Selecting meteorological records...", color = Color.White)
            }
        }
    }
}

@Composable
fun ComparisonRowSpec(
    title: String,
    valA: String,
    valB: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = valA,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )

        Text(
            text = title,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.Center
        )

        Text(
            text = valB,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}
