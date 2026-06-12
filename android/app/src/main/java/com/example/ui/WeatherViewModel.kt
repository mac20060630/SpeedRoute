package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class WeatherInfo(
    val temperature: Double,
    val windspeed: Double,
    val condition: String,
    val locationName: String,
    val isError: Boolean = false,
    val errorMessage: String = ""
)

class WeatherViewModel : ViewModel() {
    var searchQuery by mutableStateOf("")
    var weatherInfo by mutableStateOf<WeatherInfo?>(null)
    var isLoading by mutableStateOf(false)

    fun fetchWeatherForDestination(city: String) {
        if (city.isBlank()) return
        
        isLoading = true
        weatherInfo = null
        
        viewModelScope.launch {
            try {
                // 1. Geocoding
                val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=${city.replace(" ", "+")}&count=1&language=en&format=json"
                val geoJsonStr = performGetRequest(geoUrl)
                val geoJson = JSONObject(geoJsonStr)
                
                if (!geoJson.has("results")) {
                    weatherInfo = WeatherInfo(0.0, 0.0, "", "", isError = true, errorMessage = "Destination not found.")
                    isLoading = false
                    return@launch
                }
                
                val resultObj = geoJson.getJSONArray("results").getJSONObject(0)
                val lat = resultObj.getDouble("latitude")
                val lng = resultObj.getDouble("longitude")
                val locationName = resultObj.getString("name") + ", " + resultObj.optString("country", "")

                // 2. Weather
                val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lng&current_weather=true"
                val weatherJsonStr = performGetRequest(weatherUrl)
                val weatherJson = JSONObject(weatherJsonStr)
                val current = weatherJson.getJSONObject("current_weather")
                
                val temp = current.getDouble("temperature")
                val wind = current.getDouble("windspeed")
                val code = current.getInt("weathercode")
                val condition = mapWeatherCodeToCondition(code)
                
                weatherInfo = WeatherInfo(
                    temperature = temp,
                    windspeed = wind,
                    condition = condition,
                    locationName = locationName
                )
            } catch (e: Exception) {
                weatherInfo = WeatherInfo(0.0, 0.0, "", "", isError = true, errorMessage = "Failed to fetch weather data.")
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun performGetRequest(urlString: String): String = withContext(Dispatchers.IO) {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        
        if (conn.responseCode == 200) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            throw Exception("HTTP Error: ${conn.responseCode}")
        }
    }

    private fun mapWeatherCodeToCondition(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1, 2, 3 -> "Partly cloudy"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            71, 73, 75 -> "Snow fall"
            95, 96, 99 -> "Thunderstorm"
            else -> "Unknown"
        }
    }
}
