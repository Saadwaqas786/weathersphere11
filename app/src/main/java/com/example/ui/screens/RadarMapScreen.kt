package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.WeatherViewModel
import com.example.ui.components.InteractiveRadarMap

@Composable
fun RadarMapScreen(
    viewModel: WeatherViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedWeatherData by viewModel.selectedWeatherData.collectAsStateWithLifecycle()
    var selectedLayerByText by remember { mutableStateOf("Radar") }
    var isLiveFeedAnimating by remember { mutableStateOf(true) }

    val layers = listOf("Radar", "Rain", "Wind", "Temperature", "Satellite", "Cloud")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header row with back icon ---
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
            Column {
                Text(
                    text = "Atmospheric Doppler Maps",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                selectedWeatherData?.cityName?.let { activeCity ->
                    Text(
                        text = "Viewing weather maps for $activeCity",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // --- Active Layer selection slider Row ---
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(Icons.Default.Layers, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SELECT WEATHER MAP LAYER:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(layers) { layer ->
                    val isSelected = selectedLayerByText == layer
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedLayerByText = layer },
                        label = { Text(layer, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF8E44AD),
                            selectedLabelColor = Color.White,
                            containerColor = Color.White.copy(alpha = 0.12f),
                            labelColor = Color.LightGray
                        )
                    )
                }
            }
        }

        // --- Main rotating Sweep Doppler radar canvas frame ---
        InteractiveRadarMap(
            mapLayerType = selectedLayerByText,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        // --- Bottom Radar Map Console bar ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Doppler Velocity Sweep",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Real-time interactive projections",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = { isLiveFeedAnimating = !isLiveFeedAnimating },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLiveFeedAnimating) Color(0xFFE74C3C) else Color(0xFF2ECC71)
                    )
                ) {
                    Icon(
                        imageVector = if (isLiveFeedAnimating) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isLiveFeedAnimating) "Pause Sweep" else "Resume Sweep",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
