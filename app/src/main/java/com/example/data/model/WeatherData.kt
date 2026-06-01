package com.example.data.model

import java.io.Serializable

data class WeatherData(
    val cityName: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val temp: Float,
    val feelsLike: Float,
    val condition: WeatherCondition,
    val conditionDescription: String,
    val humidity: Int, // in %
    val windSpeed: Float, // in km/h
    val windDirection: String, // E, W, N, S, NE, etc.
    val visibility: Float, // in km
    val uvIndex: Int, // 0 - 11+
    val pressure: Int, // hPa
    val dewPoint: Float, // °C
    val sunrise: String, // HH:MM AM/PM
    val sunset: String,
    val aqi: Int, // Air Quality Index (0 - 500)
    val aqiLabel: String, // Good, Moderate, Unhealthy, etc.
    val pollenTree: String, // Low, Moderate, High
    val pollenGrass: String,
    val moonPhase: String, // Waxing Gibbous, New Moon, etc.
    val moonPhaseAge: Double, // Day count of moon cycle (0-29.5)
    val tideHigh: String, // Next High Tide e.g. "02:14 PM (1.8m)"
    val tideLow: String, // Next Low Tide e.g. "08:35 AM (0.3m)"
    val hourlyForecast: List<HourlyForecastItem>,
    val dailyForecast: List<DailyForecastItem>,
    val alerts: List<WeatherAlert>
) : Serializable

enum class WeatherCondition {
    SUNNY,
    CLOUDY,
    RAINY,
    SNOW,
    THUNDERSTORM,
    FOG,
    NIGHT
}

data class HourlyForecastItem(
    val time: String, // e.g. "12:00 PM"
    val temp: Float,
    val condition: WeatherCondition,
    val iconRes: String,
    val rainProbability: Int, // in %
    val windSpeed: Float,
    val humidity: Int
) : Serializable

data class DailyForecastItem(
    val dayName: String, // e.g. "Monday"
    val dateLabel: String, // e.g. "Jun 2"
    val tempMax: Float,
    val tempMin: Float,
    val condition: WeatherCondition,
    val iconRes: String,
    val rainProbability: Int, // in %
    val uvIndex: Int,
    val humidity: Int,
    val summary: String // e.g. "Showers in afternoon"
) : Serializable

data class WeatherAlert(
    val id: String,
    val title: String,
    val sender: String,
    val severity: AlertSeverity,
    val description: String,
    val timeLabel: String // e.g. "Active until 6:00 PM"
) : Serializable

enum class AlertSeverity {
    INFO,
    WARNING,
    SEVERE,
    EXTREME
}
