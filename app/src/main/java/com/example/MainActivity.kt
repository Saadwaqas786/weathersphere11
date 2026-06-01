package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.WeatherCondition
import com.example.ui.WeatherViewModel
import com.example.ui.WeatherViewModelFactory
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.WeatherThemeHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                WeatherAppMainContainer()
            }
        }
    }
}

@Composable
fun WeatherAppMainContainer() {
    val context = LocalContext.current
    val factory = remember { WeatherViewModelFactory(context) }
    val viewModel: WeatherViewModel = viewModel(factory = factory)
    
    val selectedWeather by viewModel.selectedWeatherData.collectAsState()
    
    // Manage active tab index
    var activeTabIndex by remember { mutableStateOf(0) }

    // Dynamic background brush calculated based on current active city weather condition!
    val bgBrush = WeatherThemeHelper.getGradientForCondition(
        selectedWeather?.condition ?: WeatherCondition.SUNNY
    )

    // Permission launcher for GPS lookup
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            Toast.makeText(context, "GPS permission authorized. Seeking satellite signals...", Toast.LENGTH_SHORT).show()
            viewModel.loadWeatherFromGps { fallbackMsg ->
                Toast.makeText(context, fallbackMsg, Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "GPS permission denied. Loading default city weather instead.", Toast.LENGTH_LONG).show()
        }
    }

    // Direct permissions check on start
    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine) {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        } else {
            viewModel.loadWeatherFromGps { /* ignore */ }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_main_container"),
        containerColor = Color.Transparent,
        bottomBar = {
            // Only show bottom navigation on mobile viewports (Width <= 650.dp)
            BoxWithConstraints {
                if (maxWidth <= 650.dp) {
                    NavigationBar(
                        containerColor = Color(0x33000000),
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("bottom_nav_bar")
                            .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.12f)),
                        tonalElevation = 0.dp
                    ) {
                        // Tab 0: Dashboard (My Sky)
                        NavigationBarItem(
                            selected = activeTabIndex == 0,
                            onClick = { activeTabIndex = 0 },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                            label = { Text("My Sky", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                indicatorColor = Color.White.copy(alpha = 0.15f),
                                unselectedIconColor = Color.White.copy(alpha = 0.5f),
                                unselectedTextColor = Color.White.copy(alpha = 0.5f)
                            )
                        )

                        // Tab 1: Detailed Prognosis
                        NavigationBarItem(
                            selected = activeTabIndex == 1,
                            onClick = { activeTabIndex = 1 },
                            icon = { Icon(if (activeTabIndex == 1) Icons.Filled.BarChart else Icons.Default.ShowChart, contentDescription = "Detail Forecast") },
                            label = { Text("Outlook", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                indicatorColor = Color.White.copy(alpha = 0.15f),
                                unselectedIconColor = Color.White.copy(alpha = 0.5f),
                                unselectedTextColor = Color.White.copy(alpha = 0.5f)
                            )
                        )

                        // Tab 2: AI Weather Assistant chat
                        NavigationBarItem(
                            selected = activeTabIndex == 2,
                            onClick = { activeTabIndex = 2 },
                            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant") },
                            label = { Text("AI Portal", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                indicatorColor = Color.White.copy(alpha = 0.15f),
                                unselectedIconColor = Color.White.copy(alpha = 0.5f),
                                unselectedTextColor = Color.White.copy(alpha = 0.5f)
                            )
                        )

                        // Tab 3: Interactive Maps
                        NavigationBarItem(
                            selected = activeTabIndex == 3,
                            onClick = { activeTabIndex = 3 },
                            icon = { Icon(Icons.Default.Map, contentDescription = "Doppler Maps") },
                            label = { Text("Doppler", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                indicatorColor = Color.White.copy(alpha = 0.15f),
                                unselectedIconColor = Color.White.copy(alpha = 0.5f),
                                unselectedTextColor = Color.White.copy(alpha = 0.5f)
                            )
                        )

                        // Tab 4: Inter-city Comparison
                        NavigationBarItem(
                            selected = activeTabIndex == 4,
                            onClick = { activeTabIndex = 4 },
                            icon = { Icon(Icons.Default.CompareArrows, contentDescription = "Comparisons") },
                            label = { Text("Duo View", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                indicatorColor = Color.White.copy(alpha = 0.15f),
                                unselectedIconColor = Color.White.copy(alpha = 0.5f),
                                unselectedTextColor = Color.White.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val scopeWidth = maxWidth

                if (scopeWidth > 650.dp) {
                    // --- Responsive Adaptive Wide Screen Layout (Tablet/Desktop Split Display) ---
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp, bottom = 16.dp)
                    ) {
                        // Left sidebar: Fixed Dashboard screen for navigation + favorite entries list
                        Box(
                            modifier = Modifier
                                .width(340.dp)
                                .fillMaxHeight()
                                .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.12f))
                        ) {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateDetail = { activeTabIndex = 1 },
                                onNavigateRadar = { activeTabIndex = 3 },
                                onNavigateAi = { activeTabIndex = 2 }
                            )
                        }

                        // Right layout pane showing the details screen selected by the Navigation Tabs
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            AnimatedContent(
                                targetState = activeTabIndex,
                                transitionSpec = {
                                    fadeIn() togetherWith fadeOut()
                                },
                                label = "SplitScreenTransition"
                            ) { targetIndex ->
                                when (targetIndex) {
                                    0 -> {
                                        // On wide screens, Dashboard is also shown on the left!
                                        // So we can show the Prognosis forecasts directly in the right main panel!
                                        WeatherDetailScreen(viewModel = viewModel, onNavigateBack = { activeTabIndex = 0 })
                                    }
                                    1 -> WeatherDetailScreen(viewModel = viewModel, onNavigateBack = { activeTabIndex = 0 })
                                    2 -> AiAssistantScreen(viewModel = viewModel, onNavigateBack = { activeTabIndex = 0 })
                                    3 -> RadarMapScreen(viewModel = viewModel, onNavigateBack = { activeTabIndex = 0 })
                                    4 -> WeatherComparisonScreen(viewModel = viewModel, onNavigateBack = { activeTabIndex = 0 })
                                }
                            }
                        }
                    }
                } else {
                    // --- Standard Mobile Layout with bottom tab navigation ---
                    AnimatedContent(
                        targetState = activeTabIndex,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "MobileScreenTransition"
                    ) { targetIndex ->
                        when (targetIndex) {
                            0 -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateDetail = { activeTabIndex = 1 },
                                onNavigateRadar = { activeTabIndex = 3 },
                                onNavigateAi = { activeTabIndex = 2 }
                            )
                            1 -> WeatherDetailScreen(viewModel = viewModel, onNavigateBack = { activeTabIndex = 0 })
                            2 -> AiAssistantScreen(viewModel = viewModel, onNavigateBack = { activeTabIndex = 0 })
                            3 -> RadarMapScreen(viewModel = viewModel, onNavigateBack = { activeTabIndex = 0 })
                            4 -> WeatherComparisonScreen(viewModel = viewModel, onNavigateBack = { activeTabIndex = 0 })
                        }
                    }
                }
            }
        }
    }
}
