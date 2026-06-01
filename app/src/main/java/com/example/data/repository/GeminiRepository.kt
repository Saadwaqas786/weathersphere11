package com.example.data.repository

import com.example.BuildConfig
import com.example.data.model.WeatherData
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini API Models for Moshi ---
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiConfig? = null
)

data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

data class GeminiPart(
    @Json(name = "text") val text: String
)

data class GeminiConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = null
)

data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

// --- Retrofit Service ---
interface GeminiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiRepository {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val api: GeminiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiService::class.java)
    }

    /**
     * Checks if the API key is active/valid.
     */
    fun hasValidApiKey(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * General content generation with a reliable local fallback.
     */
    suspend fun generateContent(systemInstructions: String, prompt: String, fallbackResponse: String): String = withContext(Dispatchers.IO) {
        if (!hasValidApiKey()) {
            return@withContext getSimulatedAiResponse(systemInstructions, prompt, fallbackResponse)
        }

        try {
            // Incorporating systemInstructions into the developer instruction or prepending it safely to prompt
            val fullPrompt = "$systemInstructions\n\nUser Question: $prompt"
            val request = GeminiRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(fullPrompt)))),
                generationConfig = GeminiConfig(temperature = 0.7f, maxOutputTokens = 800)
            )
            val response = api.generateContent(BuildConfig.GEMINI_API_KEY, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: fallbackResponse
        } catch (e: Exception) {
            e.printStackTrace()
            getSimulatedAiResponse(systemInstructions, prompt, fallbackResponse)
        }
    }

    /**
     * Generate custom AI weather insights based on a city's current weather data.
     */
    suspend fun getWeatherInsights(data: WeatherData): String {
        val system = "You are WeatherSphere AI, a world-class meteorological intelligence assistant. Format your response into a highly professional, engaging paragraph (maximum 150 words). Provide a high-level summary of the outlook, trends, and temperature comparison."
        val prompt = """
            Generate brief weekly weather insights for the city of ${data.cityName}, ${data.country}. 
            Current Weather: ${data.temp}°C, feels like ${data.feelsLike}°C, condition: ${data.conditionDescription}.
            Humidity: ${data.humidity}%, Wind: ${data.windSpeed} km/h ${data.windDirection}, UV Index: ${data.uvIndex}, AQI: ${data.aqi} (${data.aqiLabel}).
            Weather forecasts suggest high levels of pollen ${data.pollenTree} / grass ${data.pollenGrass}.
            Provide some rich trends or comparisons, e.g. how it behaves or feels relative to standard conditions, and a 14-day overview hint.
        """.trimIndent()

        val localFallback = """
            ${data.cityName} presents ${data.temp.toInt()}°C weather with a feels-like temperature of ${data.feelsLike.toInt()}°C under ${data.conditionDescription.lowercase()} skies. Air Quality is ${data.aqiLabel} (Index ${data.aqi}), accompanied by a wind speed of ${data.windSpeed.toInt()} km/h blowing ${data.windDirection}. With pollen activity at ${data.pollenTree.lowercase()} levels, this marks a great day for standard activities. Over the next fortnight, temperatures will fluctuate slightly around the seasonal median, with rain probability peaking later in the week.
        """.trimIndent()

        return generateContent(system, prompt, localFallback)
    }

    /**
     * Generate smart recommendations (Clothing, Outer activity, Travel, Health/AQI) in a clean structured list.
     */
    suspend fun getRecommendations(data: WeatherData): WeatherRecommendations = withContext(Dispatchers.IO) {
        val system = "You are WeatherSphere AI. Return a response containing clothing, outdoor activity, travel, and health recommendations based on weather data."
        val prompt = """
            For ${data.cityName}: Temp: ${data.temp}°C, condition: ${data.conditionDescription}, AQI: ${data.aqi} (${data.aqiLabel}), UV Index: ${data.uvIndex}, Humidity: ${data.humidity}%. 
            Provide 4 specific recommendations, one for each category:
            1. Clothing (what exactly to wear)
            2. Outdoor Activities (good times, which activities or warning)
            3. Travel suggestions (driving safety, delays)
            4. Health & Safety (UV/AQI shield, allergies)
        """.trimIndent()

        val fallbackClothing = when {
            data.temp < 10 -> "Shield yourself with heavy layers, a wool coat, windproof gloves, and a warm scarf."
            data.temp < 18 -> "Opt for a light jacket, cardigans, or comfortable sweaters paired with jeans."
            data.condition == com.example.data.model.WeatherCondition.RAINY -> "Wear a robust waterproof raincoat, high-traction boots, and keep a strong umbrella at hand."
            else -> "Wear light breathable apparel, cotton fabrics, and sunglasses for hot sunny intervals."
        }

        val fallbackOutdoor = when {
            data.condition == com.example.data.model.WeatherCondition.RAINY || data.condition == com.example.data.model.WeatherCondition.THUNDERSTORM -> 
                "Outdoor sports and cycling are discouraged. Indulge in cozy indoor exercises, gallery visits, or yoga."
            data.aqi > 100 -> "Air quality is degraded. Restrict heavy physical workouts outdoors. Consider early morning gym sessions."
            data.uvIndex > 7 -> "Perfect for morning or late afternoon runs. Minimize exposure during peak solar hours (11:00 AM - 3:00 PM)."
            else -> "Conditions are excellent for jogging, lawn tennis, hiking, or a peaceful afternoon stroll in local parks."
        }

        val fallbackTravel = when {
            data.condition == com.example.data.model.WeatherCondition.FOG -> "Dense fog reduces visibility to ${data.visibility} km. Engage low-beam fog lights, double your braking gaps, and look out for lane warnings."
            data.condition == com.example.data.model.WeatherCondition.THUNDERSTORM -> "Watch out for flash flooding hazards, wet hydroplaning highway channels, and potential aviation gusts."
            else -> "Clear airways and dry tarmac make for spectacular driving conditions. Standard transit remains perfectly on schedule."
        }

        val fallbackHealth = when {
            data.aqi > 100 -> "High ambient particulate matter (${data.aqi}). Wear an N95 filtering mask if you have respiratory sensitivities and close indoor ventilations."
            data.uvIndex > 6 -> "UV Index is at a dangerous ${data.uvIndex} level. Apply high-factor SPF 30+ sunscreen, wear a wide-brimmed cap, and drink plenty of fluids."
            data.pollenTree == "High" || data.pollenGrass == "High" -> "Pollen count is elevated. Take standard antihistamines beforehand and rinse your face after returning from a walk."
            else -> "AQI looks highly wholesome. Pollen and respiratory triggers remain exceptionally low, promoting excellent wellbeing."
        }

        // Try getting a response from Gemini to extract or parse if live key is available, else return fallbacks
        if (!hasValidApiKey()) {
            return@withContext WeatherRecommendations(fallbackClothing, fallbackOutdoor, fallbackTravel, fallbackHealth)
        }

        try {
            val responseText = generateContent(system, prompt, "")
            if (responseText.isBlank()) {
                return@withContext WeatherRecommendations(fallbackClothing, fallbackOutdoor, fallbackTravel, fallbackHealth)
            }

            // Extract lines or sections intelligently
            val lines = responseText.split("\n").filter { it.isNotBlank() }
            var clothing = fallbackClothing
            var outdoor = fallbackOutdoor
            var travel = fallbackTravel
            var health = fallbackHealth

            for (line in lines) {
                val cleaned = line.replace(Regex("^[*\\-\\d.\\s]+(Clothing|Outdoor|Travel|Health|Safety|Activities|Recommendations)?[\\s:]+"), "").trim()
                when {
                    line.contains("clothing", ignoreCase = true) || line.contains("wear", ignoreCase = true) -> clothing = cleaned
                    line.contains("outdoor", ignoreCase = true) || line.contains("activit", ignoreCase = true) -> outdoor = cleaned
                    line.contains("travel", ignoreCase = true) || line.contains("driv", ignoreCase = true) || line.contains("flight", ignoreCase = true) -> travel = cleaned
                    line.contains("health", ignoreCase = true) || line.contains("uv", ignoreCase = true) || line.contains("aqi", ignoreCase = true) || line.contains("allerg", ignoreCase = true) -> health = cleaned
                }
            }

            WeatherRecommendations(clothing, outdoor, travel, health)
        } catch (e: Exception) {
            WeatherRecommendations(fallbackClothing, fallbackOutdoor, fallbackTravel, fallbackHealth)
        }
    }

    /**
     * Local backup AI generator to make the conversation fun and educational with deep local weather awareness.
     */
    private fun getSimulatedAiResponse(systemInstructions: String, prompt: String, defaultAnswer: String): String {
        val city = Regex("City:\\s*([^,]+)").find(systemInstructions)?.groupValues?.get(1) ?: "your active location"
        val temp = Regex("Temp:\\s*([^,°]+)").find(systemInstructions)?.groupValues?.get(1) ?: "moderate"
        val condition = Regex("condition:\\s*([^,]+)").find(systemInstructions)?.groupValues?.get(1)?.lowercase() ?: "pleasant"
        val humidity = Regex("Humidity:\\s*([^%]+)").find(systemInstructions)?.groupValues?.get(1) ?: "moderate levels"
        val winds = Regex("Winds:\\s*([^,]+)").find(systemInstructions)?.groupValues?.get(1) ?: "steady flows"
        val uv = Regex("UV:\\s*([^,.]+)").find(systemInstructions)?.groupValues?.get(1)?.trim()?.toIntOrNull() ?: 4
        val aqi = Regex("AQI:\\s*([^,.]+)").find(systemInstructions)?.groupValues?.get(1)?.trim()?.toIntOrNull() ?: 45

        val hasPrecipitation = condition.contains("rain") || condition.contains("storm") || condition.contains("drizzle") || condition.contains("shower") || condition.contains("snow")

        val q = prompt.lowercase()
        return when {
            q.contains("will it rain") -> {
                if (hasPrecipitation) {
                    "Yes, current skies in $city indicate active precipitation ($condition). Relative humidity is sitting at $humidity% with wind channels at $winds. Make sure to gear up with robust rainwear or plan your activities indoors today! 🌧"
                } else {
                    "Analysis of the current microclimate for $city shows $condition conditions. No precipitation or rain peaks are indicated at this time. It looks like a dry, pleasant envelope for today's goals! ☀️"
                }
            }
            q.contains("umbrella") -> {
                if (hasPrecipitation) {
                    "Definitely! Given the $condition skies and high humidity (~$humidity%) in $city, bringing an umbrella is highly recommended. Wind currents are around $winds, so keep a sturdy grip! ☂️"
                } else {
                    "You likely won't need an umbrella today or tomorrow. Skies over $city are currently $condition with comfortable humectant saturation. Carrying a light breathable outer shell is sufficient! 🧥"
                }
            }
            q.contains("run") || q.contains("jog") || q.contains("exercise") -> {
                when {
                    aqi > 100 -> "Air standard in $city is currently degraded at AQI $aqi ($condition skies). Jogging or running outdoors is not advised due to particulate volume; we suggest switching to an indoor cardio loop. 🏃‍♂️❌"
                    uv > 7 -> "The current solar UV load in $city is a high $uv. If you decide to go for a run, avoid the peak solar heating window (11:00 AM - 3:00 PM), wear high SPF sunscreen, and carry plenty of hydration! 🧴🏃‍♂️"
                    hasPrecipitation -> "Wet roadways and slippery channels ($condition skies in $city) introduce sports hazards. We suggest performing indoor bodyweight sets or comfortable yoga routines today. 🧘‍♀️"
                    else -> "Absolutely! The temperature is a comfortable $temp°C in $city with gentle $winds. It is an optimal window for an aerobic run or jogging loop to enjoy the refreshing $condition atmosphere! 🏃‍♂️✨"
                }
            }
            q.contains("outdoor") || q.contains("sports") || q.contains("recreation") || q.contains("activities") -> {
                when {
                    aqi > 100 -> "With a degraded AQI level of $aqi in $city, target clean indoor recreational spaces today. Good outdoor intervals are restricted, but if necessary, choose the very early morning hours when pollution levels are lowest."
                    uv > 6 -> "With high UV intensity ($uv) over $city, the best hours for outdoor recreation are early morning (before 10:00 AM) or late afternoon (after 4:30 PM). If going out during midday, apply SPF indices and seek generous shade! 🌳"
                    hasPrecipitation -> "Current moist outlooks ($condition skies in $city) make indoor recreation (museum galleries, cozy reading, or indoor sports complexes) a much more comforting selection."
                    else -> "Conditions in $city are splendid! The temperature is around $temp°C under comforting $condition skies. Late morning or mid-afternoon (10:00 AM to 3:30 PM) is fully prime for hikes, tennis, or picnics. 🌲"
                }
            }
            q.contains("travel") || q.contains("flight") || q.contains("trip") || q.contains("drive") || q.contains("road") || q.contains("plans") -> {
                when {
                    condition.contains("fog") || condition.contains("mist") -> "Dense fog reduces local horizontal visibilities in $city. Drive with fog beams engaged, keep a generous separation interval, and expect minor transit schedule shifts. 🌫️🚗"
                    hasPrecipitation -> "Roadway wetness and minor aquatic pooling under $condition conditions are present in $city. Highway speeds are slightly slower, and regional airport gates may experience brief holding patterns. Pack extra patience! ✈️🌧️"
                    else -> "Excellent! Atmospheric pressure and dry tarmac pathways make for outstanding driving or travel conditions around $city today. Clear routes are reported with zero meteorological delay factors! 🚗💚"
                }
            }
            q.contains("clothing") || q.contains("wear") || q.contains("apparel") || q.contains("outfit") -> {
                val tempVal = temp.replace(Regex("[^0-9.\\-]"), "").toDoubleOrNull() ?: 20.0
                when {
                    tempVal < 10 -> "Shield yourself with heavy layers, a wool coat, windproof gloves, and a cozy scarf in $city today. Thermal conservation is essential!"
                    tempVal < 18 -> "A light jacket, knitted cardigan, or comfortable sweatshirt paired with heavier trousers is ideal for $city's $temp°C air."
                    hasPrecipitation -> "Opt for waterproof parkas, water-resistant footwear, and keep an umbrella or hooded shell ready to block moisture."
                    else -> "Wear light, highly breathable cotton apparel, a comfortable tee, sunglasses, and a cap for peak comfort in the $temp°C current heat."
                }
            }
            else -> {
                if (defaultAnswer.isNotBlank()) defaultAnswer else "Greetings! I am WeatherSphere AI, your meteorological assistant. Current active parameters for $city highlight a temperature of $temp°C under $condition skies, with an AQI of $aqi and a UV force of $uv. Ask me about travel, running, umbrellas, or outdoor time! ✨"
            }
        }
    }
}

data class WeatherRecommendations(
    val clothing: String,
    val outdoorActivities: String,
    val travel: String,
    val healthAndSafety: String
)
