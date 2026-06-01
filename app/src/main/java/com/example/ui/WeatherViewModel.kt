package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.SavedLocation
import com.example.data.database.SearchHistory
import com.example.data.model.WeatherData
import com.example.data.repository.GeminiRepository
import com.example.data.repository.WeatherRecommendations
import com.example.data.repository.WeatherRepository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WeatherViewModel(context: Context) : ViewModel() {

    private val db = AppDatabase.getDatabase(context)
    private val locationDao = db.locationDao()
    private val historyDao = db.searchHistoryDao()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // --- State Observables ---
    private val _selectedWeatherData = MutableStateFlow<WeatherData?>(null)
    val selectedWeatherData: StateFlow<WeatherData?> = _selectedWeatherData.asStateFlow()

    private val _isWeatherLoading = MutableStateFlow(false)
    val isWeatherLoading: StateFlow<Boolean> = _isWeatherLoading.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _aiInsights = MutableStateFlow<String>("")
    val aiInsights: StateFlow<String> = _aiInsights.asStateFlow()

    private val _recommendations = MutableStateFlow<WeatherRecommendations?>(null)
    val recommendations: StateFlow<WeatherRecommendations?> = _recommendations.asStateFlow()

    // --- Favorite Cities ---
    val savedLocations: StateFlow<List<SavedLocation>> = locationDao.getAllLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // A live cache map of favorite city names -> current weather details
    private val _savedLocationsWeather = MutableStateFlow<Map<String, WeatherData>>(emptyMap())
    val savedLocationsWeather: StateFlow<Map<String, WeatherData>> = _savedLocationsWeather.asStateFlow()

    // --- Search & History ---
    val searchHistory: StateFlow<List<SearchHistory>> = historyDao.getRecentSearches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchResults = MutableStateFlow<List<String>>(emptyList())
    val searchResults: StateFlow<List<String>> = _searchResults.asStateFlow()

    // --- AI Chat Assistant ---
    private val _aiChatHistory = MutableStateFlow<List<AiChatMessage>>(listOf(
        AiChatMessage("WeatherSphere AI", "Hello! I am WeatherSphere AI. Ask me about will it rain today, recommendations, outdoor sports, clothing tips, or anything else about your weather plans! ✨", isUser = false)
    ))
    val aiChatHistory: StateFlow<List<AiChatMessage>> = _aiChatHistory.asStateFlow()

    // --- City Comparison state ---
    private val _compCityAData = MutableStateFlow<WeatherData?>(null)
    val compCityAData: StateFlow<WeatherData?> = _compCityAData.asStateFlow()

    private val _compCityBData = MutableStateFlow<WeatherData?>(null)
    val compCityBData: StateFlow<WeatherData?> = _compCityBData.asStateFlow()

    init {
        // Load default city on startup
        selectCity("New York")
        
        // Listen to savedLocations to keep the weather cache populated
        viewModelScope.launch {
            savedLocations.collect { list ->
                updateWeatherCacheForFavorites(list)
            }
        }
    }

    /**
     * Load weather data for a city, update recent searches, and fetch AI insights.
     */
    fun selectCity(name: String) {
        viewModelScope.launch {
            _isWeatherLoading.value = true
            _errorMessage.value = null
            try {
                val data = WeatherRepository.getWeatherForCity(name)
                _selectedWeatherData.value = data
                
                // Add to recent searches (don't save placeholders)
                if (name != "GPS Coordinate" && !name.contains("(Near)")) {
                    historyDao.insertSearch(SearchHistory(name, System.currentTimeMillis()))
                }

                // Fetch AI Insights and Recommendations in parallel
                fetchAiDetails(data)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load meteorological data: ${e.message}"
            } finally {
                _isWeatherLoading.value = false
            }
        }
    }

    /**
     * Fetch AI comments asynchronously.
     */
    private fun fetchAiDetails(data: WeatherData) {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                // Run call triggers
                val insights = GeminiRepository.getWeatherInsights(data)
                _aiInsights.value = insights

                val recs = GeminiRepository.getRecommendations(data)
                _recommendations.value = recs
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    /**
     * Cache favorite weather instances.
     */
    private fun updateWeatherCacheForFavorites(list: List<SavedLocation>) {
        viewModelScope.launch {
            val cache = _savedLocationsWeather.value.toMutableMap()
            list.forEach { saved ->
                if (!cache.containsKey(saved.cityName)) {
                    try {
                        val wData = WeatherRepository.getWeatherForCity(saved.cityName)
                        cache[saved.cityName] = wData
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            _savedLocationsWeather.value = cache
        }
    }

    /**
     * Autocomplete searching.
     */
    fun searchCityQuery(query: String) {
        viewModelScope.launch {
            if (query.length < 2) {
                _searchResults.value = emptyList()
                return@launch
            }
            // Filter preset cities
            val matched = WeatherRepository.PRESET_CITIES.filter {
                it.name.contains(query, ignoreCase = true) || it.country.contains(query, ignoreCase = true)
            }.map { it.name }
            
            // Add a dynamic entry if not present
            val lowercaseQuery = query.trim().lowercase()
            val list = matched.toMutableList()
            if (list.none { it.lowercase() == lowercaseQuery }) {
                list.add(query.trim().capitalize())
            }
            _searchResults.value = list
        }
    }

    /**
     * Save a location to Room database Favorites.
     */
    fun toggleFavorite(cityName: String, country: String, lat: Double, lon: Double) {
        viewModelScope.launch {
            val isFav = locationDao.containsCity(cityName) > 0
            if (isFav) {
                locationDao.deleteLocationByName(cityName)
                // Remove from cache
                val cache = _savedLocationsWeather.value.toMutableMap()
                cache.remove(cityName)
                _savedLocationsWeather.value = cache
            } else {
                locationDao.insertLocation(SavedLocation(cityName, country, lat, lon))
                // Warm up cache
                try {
                    val data = WeatherRepository.getWeatherForCity(cityName)
                    val cache = _savedLocationsWeather.value.toMutableMap()
                    cache[cityName] = data
                    _savedLocationsWeather.value = cache
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun isFavorite(cityName: String): Flow<Boolean> = flow {
        emit(locationDao.containsCity(cityName) > 0)
    }

    fun deleteRecentSearch(query: String) {
        viewModelScope.launch {
            historyDao.deleteSearchByQuery(query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            historyDao.clearHistory()
        }
    }

    /**
     * GPS location retrieval and loading.
     */
    @SuppressLint("MissingPermission")
    fun loadWeatherFromGps(onFallbackMessage: (String) -> Unit) {
        _isWeatherLoading.value = true
        _errorMessage.value = null
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    viewModelScope.launch {
                        if (location != null) {
                            val data = WeatherRepository.getWeatherForCoordinates(
                                location.latitude,
                                location.longitude
                            )
                            _selectedWeatherData.value = data
                            fetchAiDetails(data)
                        } else {
                            // Fallback if location returned null (e.g. fresh emulator or disabled GPS)
                            onFallbackMessage("GPS satellite coordinates not found. Loading New York weather instead.")
                            selectCity("New York")
                        }
                        _isWeatherLoading.value = false
                    }
                }
                .addOnFailureListener {
                    onFallbackMessage("GPS permission/service error. Loading London forecast instead.")
                    selectCity("London")
                    _isWeatherLoading.value = false
                }
        } catch (e: SecurityException) {
            _errorMessage.value = "GPS permission required for live coordinates lookup."
            _isWeatherLoading.value = false
        }
    }

    /**
     * Ask Gemini Chat Assistant a conversational question related to current city weather.
     */
    fun askAiAssistant(question: String) {
        if (question.isBlank()) return
        
        val userMsg = AiChatMessage("You", question, isUser = true)
        _aiChatHistory.value = _aiChatHistory.value + userMsg

        viewModelScope.launch {
            val weatherData = _selectedWeatherData.value
            val contextInfo = if (weatherData != null) {
                "City: ${weatherData.cityName}, Temp: ${weatherData.temp}°C, condition: ${weatherData.conditionDescription}, Humidity: ${weatherData.humidity}%, Winds: ${weatherData.windSpeed} km/h, UV: ${weatherData.uvIndex}, AQI: ${weatherData.aqi}."
            } else {
                "No active city data selected."
            }

            val systemPrompt = """
                You are WeatherSphere AI, a polite, professional, and world-class meteorological chat counselor. 
                Use the following atmospheric context to frame your answer carefully and offer helpful hints. 
                Keep comments conversational and easy to read. Try to add relevant, elegant tips.
                Weather Context:
                $contextInfo
            """.trimIndent()

            val response = GeminiRepository.generateContent(
                systemPrompt,
                question,
                "I am currently offline or experiencing heavy service currents. Ambient conditions look stellar! I recommend wearing light layers."
            )

            val botMsg = AiChatMessage("WeatherSphere AI", response, isUser = false)
            _aiChatHistory.value = _aiChatHistory.value + botMsg
        }
    }

    /**
     * Compare two cities.
     */
    fun setComparisonCities(cityA: String, cityB: String) {
        viewModelScope.launch {
            try {
                _compCityAData.value = WeatherRepository.getWeatherForCity(cityA)
                _compCityBData.value = WeatherRepository.getWeatherForCity(cityB)
            } catch (e: Exception) {
                _errorMessage.value = "Error comparing cities: ${e.message}"
            }
        }
    }
}

data class AiChatMessage(
    val sender: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class WeatherViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WeatherViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
