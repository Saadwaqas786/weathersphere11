package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.data.repository.WeatherRepository
import com.example.ui.WeatherViewModel
import com.example.ui.components.CircularGauge
import com.example.ui.components.GlassCard
import com.example.ui.components.SevereWeatherAlertBanner
import com.example.ui.theme.WeatherThemeHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: WeatherViewModel,
    onNavigateDetail: () -> Unit,
    onNavigateRadar: () -> Unit,
    onNavigateAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val weatherData by viewModel.selectedWeatherData.collectAsStateWithLifecycle()
    val isWeatherLoading by viewModel.isWeatherLoading.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val aiInsights by viewModel.aiInsights.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    val savedFavorites by viewModel.savedLocations.collectAsStateWithLifecycle()
    val favoritesWeatherColors by viewModel.savedLocationsWeather.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()

    // Filter Widget Config items (users can hide/reorder widgets dynamic in settings)
    var showAstronomyConfig by remember { mutableStateOf(true) }
    var showPollenConfig by remember { mutableStateOf(true) }
    var showTidesConfig by remember { mutableStateOf(true) }
    var showCustomizerDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header Search Bar & GPS ---
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            viewModel.searchCityQuery(it)
                        },
                        placeholder = { Text("Search any city worldwide...", color = Color.White.copy(alpha = 0.6f)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    viewModel.searchCityQuery("")
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_text_input")
                            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // GPS Button
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Scanning satellite atmospheric position...", Toast.LENGTH_SHORT).show()
                            viewModel.loadWeatherFromGps { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .testTag("gps_locate_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "GPS Auto Locate",
                            tint = Color.White
                        )
                    }
                }

                // Autocomplete/Search dropdown overlay
                AnimatedVisibility(
                    visible = searchQuery.isNotEmpty() || isSearchFocused,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xEC1F2A38)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Column {
                            if (searchResults.isNotEmpty()) {
                                Text(
                                    text = "SUGGESTED CITIES",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
                                )
                                searchResults.forEach { matchedCity ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.selectCity(matchedCity)
                                                searchQuery = ""
                                                isSearchFocused = false
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.LightGray)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(matchedCity, color = Color.White, fontSize = 14.sp)
                                    }
                                }
                            }

                            if (searchHistory.isNotEmpty()) {
                                Divider(color = Color.White.copy(alpha = 0.1f))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "RECENT SEARCHES",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = "Clear All",
                                        fontSize = 11.sp,
                                        color = Color(0xFFF39C12),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { viewModel.clearSearchHistory() }
                                    )
                                }

                                searchHistory.forEach { history ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.selectCity(history.query)
                                                searchQuery = ""
                                                isSearchFocused = false
                                            }
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.History, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(history.query, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                        IconButton(
                                            onClick = { viewModel.deleteRecentSearch(history.query) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }

                            if (searchResults.isEmpty() && searchHistory.isEmpty()) {
                                Text(
                                    text = "Type at least 2 characters to trigger atmospheric forecasting...",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Loading and Selected City overview
        if (isWeatherLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        } else {
            weatherData?.let { data ->
                // Favorite Cities Scroll view block
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "FAVORITE CITIES",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        if (savedFavorites.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Your favorite dashboard is empty.",
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Search a city and tap the favorite heart icon to save here.",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(savedFavorites) { fav ->
                                    val favWeather = favoritesWeatherColors[fav.cityName]
                                    Card(
                                        modifier = Modifier
                                            .width(135.dp)
                                            .clickable { viewModel.selectCity(fav.cityName) },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (favWeather != null) {
                                                WeatherThemeHelper.getAccentColor(favWeather.condition).copy(alpha = 0.15f)
                                            } else {
                                                Color.White.copy(alpha = 0.1f)
                                            }
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    fav.cityName,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    maxLines = 1
                                                )
                                                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFE74C3C), modifier = Modifier.size(12.dp))
                                            }
                                            Text(
                                                fav.country,
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = favWeather?.let { "${it.temp.toInt()}°C" } ?: "--°",
                                                color = Color.White,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                            Text(
                                                text = favWeather?.conditionDescription ?: "Syncing...",
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- Severe weather warning banner if any ---
                if (data.alerts.isNotEmpty()) {
                    items(data.alerts) { alert ->
                        SevereWeatherAlertBanner(alert = alert)
                    }
                }

                // --- Main Temperature Focus Display ---
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = data.cityName,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            val isFavFlow = viewModel.isFavorite(data.cityName).collectAsState(initial = false)
                            IconButton(
                                onClick = {
                                    viewModel.toggleFavorite(data.cityName, data.country, data.latitude, data.longitude)
                                }
                            ) {
                                Icon(
                                    imageVector = if (isFavFlow.value) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Save favorite",
                                    tint = if (isFavFlow.value) Color(0xFFE74C3C) else Color.White,
                                    modifier = Modifier.size(28.dp).testTag("favorite_toggle_button")
                                )
                            }
                        }

                        Text(
                            text = data.country,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "${data.temp.toInt()}°",
                            fontSize = 80.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )

                        Text(
                            text = data.conditionDescription,
                            fontSize = 20.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Feels like ${data.feelsLike.toInt()}°C • Winds blowing ${data.windDirection} at ${data.windSpeed.toInt()} km/h",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // --- AI Insights Block (Gemini Powered) ---
                item {
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF8E44AD))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Intel", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "WEATHERSPHERE METEOROLOGY AI",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFBB8FCE)
                                )
                                Text(
                                    text = "Weekly outlook summaries & trends",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                            if (isAiLoading) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (aiInsights.isNotEmpty()) aiInsights else "Synthesizing deep meteorological parameters, humidity layers and barometric forecasts...",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.95f),
                            lineHeight = 18.sp
                        )
                    }
                }

                // Quick Navigation Grid to and other Screens
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNavigateDetail,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("14-Day Forecast", color = Color.White, fontSize = 12.sp, maxLines = 1)
                        }

                        Button(
                            onClick = onNavigateRadar,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Radar Maps", color = Color.White, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }

                // --- circular Gauges Widgets Container ---
                item {
                    Text(
                        text = "METEOROLOGICAL METRICS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CircularGauge(
                            value = (data.uvIndex / 12f).coerceIn(0f, 1f),
                            centerLabel = data.uvIndex.toString(),
                            subtitle = "UV INDEX",
                            caption = when {
                                data.uvIndex <= 2 -> "Low"
                                data.uvIndex <= 5 -> "Moderate"
                                data.uvIndex <= 7 -> "High"
                                data.uvIndex <= 10 -> "Very High"
                                else -> "Extreme"
                            },
                            gaugeColor = Color(0xFFF39C12),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularGauge(
                            value = (data.aqi / 300f).coerceIn(0f, 1f),
                            centerLabel = data.aqi.toString(),
                            subtitle = "AQI INDICATOR",
                            caption = when {
                                data.aqi <= 50 -> "Good"
                                data.aqi <= 100 -> "Moderate"
                                data.aqi <= 150 -> "Risky"
                                else -> "Unhealthy"
                            },
                            gaugeColor = Color(0xFF2ECC71),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularGauge(
                            value = data.humidity / 100f,
                            centerLabel = "${data.humidity}%",
                            subtitle = "HUMIDITY",
                            caption = when {
                                data.humidity < 40 -> "Dry"
                                data.humidity < 70 -> "Wholesome"
                                else -> "Damp"
                            },
                            gaugeColor = Color(0xFF3498DB),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // --- Smart Recommendations Cards (Gemini Generated) ---
                item {
                    recommendations?.let { recs ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "SMART AI RECOMMENDATIONS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.7f)
                            )

                            // Clothing Card
                            GlassCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Color(0xFFF1C40F))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Clothing Recommendations", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                        Text(recs.clothing, fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 16.sp)
                                    }
                                }
                            }

                            // Outdoor Card
                            GlassCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = Color(0xFF2ECC71))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Outdoor Activities Guidance", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                        Text(recs.outdoorActivities, fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 16.sp)
                                    }
                                }
                            }

                            // Travel Card
                            GlassCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color(0xFFE74C3C))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Travel & Highway Advisory", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                        Text(recs.travel, fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 16.sp)
                                    }
                                }
                            }

                            // Health Card
                            GlassCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF3498DB))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Health & Respiratory Safety", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                        Text(recs.healthAndSafety, fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Expanded Extra parameters (Astro, Tides, Pollen trackers)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ASTRONOMY & LIFE INDEXES",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        IconButton(onClick = { showCustomizerDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Customize Dashboard", tint = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }

                // Dynamic Astronomy Card
                if (showAstronomyConfig) {
                    item {
                        GlassCard {
                            Text("Astronomy & Solstice Tracking", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFFF39C12), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sunrise: ${data.sunrise}", color = Color.White, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.WbTwilight, contentDescription = null, tint = Color(0xFFE74C3C), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sunset: ${data.sunset}", color = Color.White, fontSize = 12.sp)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Nightlight, contentDescription = null, tint = Color(0xFF9E9E9E), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Moon: ${data.moonPhase}", color = Color.White, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Lunar Cycle Age: ${String.format("%.1f", data.moonPhaseAge)} Days", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                // Dynamic Tides Card
                if (showTidesConfig) {
                    item {
                        GlassCard {
                            Text("Ocean High & Low Tides", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF3498DB), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("High: ${data.tideHigh}", color = Color.White, fontSize = 12.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Color(0xFF9B59B6), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Low: ${data.tideLow}", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Dynamic Pollen Card
                if (showPollenConfig) {
                    item {
                        GlassCard {
                            Text("Grass & Tree Pollen Index Tracker", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Tree Pollen level:", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                    Text(data.pollenTree, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Grass Pollen level:", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                    Text(data.pollenGrass, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Settings Customizer Dialog definition
    if (showCustomizerDialog) {
        AlertDialog(
            onDismissRequest = { showCustomizerDialog = false },
            title = { Text("Customize Dashboard", color = Color.White, fontWeight = FontWeight.Bold) },
            containerColor = Color(0xFF1C2833),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select which widgets are shown on your custom WeatherSphere desktop:", color = Color.LightGray, fontSize = 13.sp)
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = showAstronomyConfig, onCheckedChange = { showAstronomyConfig = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Astronomy (Sun & Moon phase)", color = Color.White)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = showTidesConfig, onCheckedChange = { showTidesConfig = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ocean & High/Low Tide", color = Color.White)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = showPollenConfig, onCheckedChange = { showPollenConfig = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pollen allergen tracker", color = Color.White)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showCustomizerDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E44AD))
                ) {
                    Text("Save Layout", color = Color.White)
                }
            }
        )
    }
}
