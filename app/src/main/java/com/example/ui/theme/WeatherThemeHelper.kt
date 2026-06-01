package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.data.model.WeatherCondition

object WeatherThemeHelper {

    // Condition Colors
    val SunnySky = Color(0xFF3498DB)
    val SunnyHorizon = Color(0xFFF1C40F)
    
    val CloudySky = Color(0xFF5D6D7E)
    val CloudyHorizon = Color(0xFFBDC3C7)
    
    val RainySky = Color(0xFF2E4053)
    val RainyHorizon = Color(0xFF85929E)
    
    val SnowySky = Color(0xFF7FB3D5)
    val SnowyHorizon = Color(0xFFEBEDEF)
    
    val ThunderstormSky = Color(0xFF1F2630)
    val ThunderstormHorizon = Color(0xFF34495E)
    
    val FogSky = Color(0xFF95A5A6)
    val FogHorizon = Color(0xFFD5DBDB)
    
    val NightSky = Color(0xFF0B132B)
    val NightHorizon = Color(0xFF1C2541)

    // Glassmorphism transparent cards
    val GlassSurfaceLight = Color(0x2AFFFFFF)
    val GlassBorderLight = Color(0x3BFFFFFF)
    val GlassSurfaceDark = Color(0x3B1F202A)
    val GlassBorderDark = Color(0x1BFFFFFF)

    /**
     * Returns a premium linear gradient brush corresponding to the current Weather Condition.
     */
    fun getGradientForCondition(condition: WeatherCondition): Brush {
        return when (condition) {
            WeatherCondition.SUNNY -> Brush.verticalGradient(
                colors = listOf(Color(0xFF2E86C1), Color(0xFF5DADE2), Color(0xFFFAD7A0))
            )
            WeatherCondition.CLOUDY -> Brush.verticalGradient(
                colors = listOf(Color(0xFF34495E), Color(0xFF5D6D7E), Color(0xFFBDC3C7))
            )
            WeatherCondition.RAINY -> Brush.verticalGradient(
                colors = listOf(Color(0xFF1B2631), Color(0xFF2E4053), Color(0xFF85929E))
            )
            WeatherCondition.SNOW -> Brush.verticalGradient(
                colors = listOf(Color(0xFF2C3E50), Color(0xFF34495E), Color(0xFFAEC6CF))
            )
            WeatherCondition.THUNDERSTORM -> Brush.verticalGradient(
                colors = listOf(Color(0xFF11171D), Color(0xFF1C2833), Color(0xFF2C3E50))
            )
            WeatherCondition.FOG -> Brush.verticalGradient(
                colors = listOf(Color(0xFF515A5A), Color(0xFF7F8C8D), Color(0xFFD5DBDB))
            )
            WeatherCondition.NIGHT -> Brush.verticalGradient(
                colors = listOf(Color(0xFF0B132B), Color(0xFF1C2541), Color(0xFF3A506B))
            )
        }
    }

    /**
     * Returns an elegant color tint that can be applied to specific visual outlines and badges.
     */
    fun getAccentColor(condition: WeatherCondition): Color {
        return when (condition) {
            WeatherCondition.SUNNY -> Color(0xFFF39C12)
            WeatherCondition.CLOUDY -> Color(0xFFECF0F1)
            WeatherCondition.RAINY -> Color(0xFF5DADE2)
            WeatherCondition.SNOW -> Color(0xFFEBF5FB)
            WeatherCondition.THUNDERSTORM -> Color(0xFFF4D03F)
            WeatherCondition.FOG -> Color(0xFFBDC3C7)
            WeatherCondition.NIGHT -> Color(0xFF5DADE2)
        }
    }
}
