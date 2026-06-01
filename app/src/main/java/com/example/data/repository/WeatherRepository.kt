package com.example.data.repository

import com.example.data.model.*
import java.util.*
import kotlin.math.sin

object WeatherRepository {

    // Preset list of cities for quick dashboard showcase or autocomplete
    val PRESET_CITIES = listOf(
        PresetCity("New York", "United States", 40.7128, -74.0060, "Warm and partly cloudy, breezy along the coast."),
        PresetCity("London", "United Kingdom", 51.5074, -0.1278, "Cool temperatures with light intermittent rain."),
        PresetCity("Tokyo", "Japan", 35.6762, 139.6503, "Mild, humid with clear night skies."),
        PresetCity("Sydney", "Australia", -33.8688, 151.2093, "Chilly, crisp autumn air with mild ocean winds."),
        PresetCity("Cairo", "Egypt", 30.0444, 31.2357, "Hot and sunny, high UV indices with sand gusts."),
        PresetCity("Paris", "France", 48.8566, 2.3522, "Pleasant summer weather with light cumulus clouds."),
        PresetCity("Reykjavik", "Iceland", 64.1466, -21.9426, "Cold subarctic wind with light flurry showers.")
    )

    fun searchPresetCities(query: String): List<PresetCity> {
        if (query.isBlank()) return PRESET_CITIES
        return PRESET_CITIES.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.country.contains(query, ignoreCase = true)
        }
    }

    fun getWeatherForCity(cityName: String): WeatherData {
        val preset = PRESET_CITIES.firstOrNull { it.name.equals(cityName, ignoreCase = true) }
        val lat = preset?.lat ?: 40.0
        val lon = preset?.lon ?: -75.0
        val country = preset?.country ?: "Global"
        return generateWeather(cityName, country, lat, lon)
    }

    fun getWeatherForCoordinates(lat: Double, lon: Double): WeatherData {
        // Find nearest city or generate
        val nearest = PRESET_CITIES.minByOrNull {
            val dLat = it.lat - lat
            val dLon = it.lon - lon
            dLat * dLat + dLon * dLon
        }
        val cityName = nearest?.name?.let { "$it (Near)" } ?: "GPS Coordinate"
        val country = nearest?.country ?: "Local"
        return generateWeather(cityName, country, lat, lon)
    }

    private fun generateWeather(cityName: String, country: String, lat: Double, lon: Double): WeatherData {
        val calendar = Calendar.getInstance()
        val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

        // Base climate parameters based on latitude
        val isSouthernHemisphere = lat < 0
        val isTropical = Math.abs(lat) < 23.5
        val isArctic = Math.abs(lat) > 60.0

        // In June, Northern hemisphere is in summer, Southern is in winter
        val baseTemp = if (isTropical) {
            31f
        } else if (isArctic) {
            8f
        } else if (isSouthernHemisphere) {
            12f // Winter in Sydney
        } else {
            26f // Summer in New York / London
        }

        // Apply a deterministic offset based on city name characters so each city feels distinct and persistent
        val citySeed = cityName.hashCode().let { if (it < 0) -it else it }
        val tempOffset = (citySeed % 12) - 6f // -6 to +5
        val humiditySeed = (citySeed % 40) + 40 // 40 to 80
        val defaultConditionInt = citySeed % 6 // 0: SUNNY, 1: CLOUDY, 2: RAINY, 3: SNOW, 4: THUNDERSTORM, 5: FOG

        val condition = when {
            isArctic && defaultConditionInt == 2 -> WeatherCondition.SNOW
            isArctic && defaultConditionInt == 3 -> WeatherCondition.SNOW
            defaultConditionInt == 0 -> WeatherCondition.SUNNY
            defaultConditionInt == 1 -> WeatherCondition.CLOUDY
            defaultConditionInt == 2 -> WeatherCondition.RAINY
            defaultConditionInt == 3 -> {
                if (baseTemp + tempOffset < 2) WeatherCondition.SNOW else WeatherCondition.RAINY
            }
            defaultConditionInt == 4 -> WeatherCondition.THUNDERSTORM
            else -> WeatherCondition.FOG
        }

        val finalCondition = if (hour24 !in 6..19 && condition == WeatherCondition.SUNNY) {
            WeatherCondition.NIGHT
        } else {
            condition
        }

        val currentTemp = baseTemp + tempOffset + (3 * sin(hour24 * Math.PI / 12 - 2)).toFloat()
        val feelsLike = currentTemp + if (humiditySeed > 70) 2f else -1f

        val conditionText = when (finalCondition) {
            WeatherCondition.SUNNY -> "Clear & Sunny"
            WeatherCondition.CLOUDY -> "Mostly Cloudy"
            WeatherCondition.RAINY -> "Showers"
            WeatherCondition.SNOW -> "Snow Flurries"
            WeatherCondition.THUNDERSTORM -> "Severe Thunderstorm"
            WeatherCondition.FOG -> "Dense Fog"
            WeatherCondition.NIGHT -> "Clear Night"
        }

        val windSpeed = 5f + (citySeed % 25) + sin(hour24.toDouble()).toFloat() * 3
        val windDirs = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        val windDirection = windDirs[citySeed % windDirs.size]

        val visibility = if (finalCondition == WeatherCondition.FOG) 1.2f else 10.0f - (humiditySeed / 25) + (citySeed % 3)

        val uvIndex = if (hour24 !in 8..17 || finalCondition == WeatherCondition.RAINY || finalCondition == WeatherCondition.SNOW) {
            0
        } else {
            val maxUv = if (isTropical) 11 else if (isSouthernHemisphere) 4 else 8
            val ratio = sin((hour24 - 6) * Math.PI / 11)
            (maxUv * ratio).toInt().coerceIn(0, 12)
        }

        val pressure = 1008 + (citySeed % 18) + if (finalCondition == WeatherCondition.THUNDERSTORM) -12 else 4
        val dewPoint = currentTemp - ((100 - humiditySeed) / 5)

        // Sunrise/sunset calculation depending on latitude
        val sunriseHour = (6.0 + if (isSouthernHemisphere) 1.5 else -0.5).toInt()
        val sunsetHour = (18.0 + if (isSouthernHemisphere) -1.5 else 1.5).toInt()
        val sunrise = String.format("%02d:15 AM", sunriseHour)
        val sunset = String.format("%02d:42 PM", sunsetHour - 12)

        // AQI calculation (Cairo is higher, Reykjavik is clean)
        val baseAqi = if (cityName.contains("Cairo")) 125 else if (cityName.contains("Reykjavik")) 15 else 48
        val aqi = (baseAqi + (citySeed % 30) + (if (windSpeed < 8) 12 else -5)).coerceIn(10, 350)
        val aqiLabel = when {
            aqi <= 50 -> "Good"
            aqi <= 100 -> "Moderate"
            aqi <= 150 -> "Unhealthy for Sensitive Groups"
            aqi <= 200 -> "Unhealthy"
            else -> "Very Unhealthy"
        }

        val pollenLabels = listOf("Low", "Moderate", "High", "Very High")
        val pollenTree = pollenLabels[(citySeed % 3) + if (finalCondition == WeatherCondition.SUNNY) 1 else 0]
        val pollenGrass = pollenLabels[((citySeed + 1) % 3) + if (finalCondition == WeatherCondition.SUNNY) 1 else 0]

        // Moon cycle calculation
        val moonAge = (dayOfYear % 29.5)
        val moonPhase = when {
            moonAge < 1.0 -> "New Moon"
            moonAge < 6.5 -> "Waxing Crescent"
            moonAge < 8.5 -> "First Quarter"
            moonAge < 14.0 -> "Waxing Gibbous"
            moonAge < 16.0 -> "Full Moon"
            moonAge < 21.5 -> "Waning Gibbous"
            moonAge < 23.5 -> "Last Quarter"
            else -> "Waning Crescent"
        }

        val tideHighHour = (14 + (citySeed % 10)) % 12
        val tideHigh = String.format("%02d:24 %s (1.9m)", if (tideHighHour == 0) 12 else tideHighHour, if (citySeed % 2 == 0) "AM" else "PM")
        val tideLowHour = (tideHighHour + 6) % 12
        val tideLow = String.format("%02d:51 %s (0.2m)", if (tideLowHour == 0) 12 else tideLowHour, if (citySeed % 2 == 0) "PM" else "AM")

        // 48 Hourly Items
        val hourlyForecast = (0 until 48).map { diff ->
            val fcHour = (hour24 + diff) % 24
            val displayHour = when {
                fcHour == 0 -> "12 AM"
                fcHour == 12 -> "12 PM"
                fcHour > 12 -> "${fcHour - 12} PM"
                else -> "$fcHour AM"
            }
            val fcTemp = currentTemp + (4 * sin((fcHour - 14) * Math.PI / 12)).toFloat() + (diff * -0.1f)
            val rainProb = if (finalCondition == WeatherCondition.RAINY || finalCondition == WeatherCondition.THUNDERSTORM) {
                (50 + 40 * sin(fcHour * Math.PI / 12)).toInt().coerceIn(20, 100)
            } else if (finalCondition == WeatherCondition.CLOUDY) {
                (20 * sin(fcHour * Math.PI / 12)).toInt().coerceIn(0, 40)
            } else {
                0
            }

            val curCond = when {
                rainProb > 60 -> WeatherCondition.RAINY
                fcHour !in 6..19 -> WeatherCondition.NIGHT
                finalCondition == WeatherCondition.CLOUDY -> WeatherCondition.CLOUDY
                else -> WeatherCondition.SUNNY
            }

            HourlyForecastItem(
                time = displayHour,
                temp = fcTemp,
                condition = curCond,
                iconRes = curCond.name,
                rainProbability = rainProb,
                windSpeed = windSpeed + sin(diff.toDouble()).toFloat() * 2f,
                humidity = (humiditySeed + sin(diff.toDouble() * 0.5) * 15).toInt().coerceIn(20, 100)
            )
        }

        // 14 Daily Items
        val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val calendarTemp = calendar.clone() as Calendar
        val dailyForecast = (0 until 14).map { diff ->
            val dayName = if (diff == 0) "Today" else days[calendarTemp.get(Calendar.DAY_OF_WEEK) - 1]
            val monthLabel = getMonthName(calendarTemp.get(Calendar.MONTH))
            val dateLabel = "$monthLabel ${calendarTemp.get(Calendar.DAY_OF_MONTH)}"
            calendarTemp.add(Calendar.DAY_OF_YEAR, 1)

            val maxDiff = 4f + sin(diff.toDouble() * 0.8).toFloat() * 3
            val minDiff = -4f - sin(diff.toDouble() * 0.8).toFloat() * 2
            val tempMax = currentTemp + maxDiff
            val tempMin = currentTemp + minDiff

            val probability = if (finalCondition == WeatherCondition.RAINY || finalCondition == WeatherCondition.THUNDERSTORM) {
                (citySeed % 40) + 40 + (diff * 2) % 20
            } else {
                (citySeed % 25)
            }.coerceIn(0, 95)

            val curCond = when {
                probability > 60 -> WeatherCondition.RAINY
                probability > 30 -> WeatherCondition.CLOUDY
                else -> WeatherCondition.SUNNY
            }

            val summary = when (curCond) {
                WeatherCondition.RAINY -> "Showers all day"
                WeatherCondition.CLOUDY -> "Partly sunny clouds"
                else -> "Perfect clear skies"
            }

            DailyForecastItem(
                dayName = dayName,
                dateLabel = dateLabel,
                tempMax = tempMax,
                tempMin = tempMin,
                condition = curCond,
                iconRes = curCond.name,
                rainProbability = probability,
                uvIndex = if (curCond == WeatherCondition.SUNNY) uvIndex + 1 else (uvIndex - 2).coerceAtLeast(0),
                humidity = (humiditySeed + (diff % 10) - 5).coerceIn(20, 100),
                summary = summary
            )
        }

        // Generate custom severe alerts based on city seed
        val alerts = mutableListOf<WeatherAlert>()
        if (finalCondition == WeatherCondition.THUNDERSTORM) {
            alerts.add(
                WeatherAlert(
                    id = "alert_storm_${citySeed}",
                    title = "Severe Thunderstorm Warning",
                    sender = "National Weather Service",
                    severity = AlertSeverity.SEVERE,
                    description = "A severe thunderstorm capable of producing high gusts, intense lightning strikes, and localized downpours is moving through the area. Residents are advised to stay indoors and secure light objects.",
                    timeLabel = "Active until 9:00 PM"
                )
            )
        } else if (aqi > 150) {
            alerts.add(
                WeatherAlert(
                    id = "alert_aqi_${citySeed}",
                    title = "Air Quality Action Alert",
                    sender = "Environmental Protection Agency",
                    severity = AlertSeverity.WARNING,
                    description = "Extremely high pollen index and pollution level detected. Sensitive individuals should minimize physical exertion outdoors.",
                    timeLabel = "Active for 48 hours"
                )
            )
        } else if (currentTemp > 35) {
            alerts.add(
                WeatherAlert(
                    id = "alert_heat_${citySeed}",
                    title = "Excessive Heat Warning",
                    sender = "Meteorological Office",
                    severity = AlertSeverity.EXTREME,
                    description = "Dangerous heat conditions expected today with ambient indices values near feels-like peaks of ${feelsLike.toInt()}°C. Please stay hydrated.",
                    timeLabel = "Active until 8:00 PM"
                )
            )
        }

        return WeatherData(
            cityName = cityName,
            country = country,
            latitude = lat,
            longitude = lon,
            temp = currentTemp,
            feelsLike = feelsLike,
            condition = finalCondition,
            conditionDescription = conditionText,
            humidity = humiditySeed,
            windSpeed = windSpeed,
            windDirection = windDirection,
            visibility = visibility,
            uvIndex = uvIndex,
            pressure = pressure,
            dewPoint = dewPoint,
            sunrise = sunrise,
            sunset = sunset,
            aqi = aqi,
            aqiLabel = aqiLabel,
            pollenTree = pollenTree,
            pollenGrass = pollenGrass,
            moonPhase = moonPhase,
            moonPhaseAge = moonAge,
            tideHigh = tideHigh,
            tideLow = tideLow,
            hourlyForecast = hourlyForecast,
            dailyForecast = dailyForecast,
            alerts = alerts
        )
    }

    private fun getMonthName(monthIndex: Int): String {
        return listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[monthIndex]
    }
}

data class PresetCity(
    val name: String,
    val country: String,
    val lat: Double,
    val lon: Double,
    val regionalHint: String
)
