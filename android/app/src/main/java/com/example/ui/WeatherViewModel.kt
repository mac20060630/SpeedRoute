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
import java.net.URLEncoder

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
                // 1. Geocoding — use URLEncoder for safe query strings
                val encodedCity = URLEncoder.encode(city.trim(), "UTF-8")
                val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=$encodedCity&count=1&language=en&format=json"
                val geoJsonStr = performGetRequest(geoUrl)
                val geoJson = JSONObject(geoJsonStr)

                if (!geoJson.has("results") || geoJson.getJSONArray("results").length() == 0) {
                    weatherInfo = WeatherInfo(
                        temperature = 0.0, windspeed = 0.0, condition = "", locationName = "",
                        isError = true, errorMessage = "City \"$city\" not found. Try a different spelling."
                    )
                    return@launch
                }

                val resultObj = geoJson.getJSONArray("results").getJSONObject(0)
                val lat = resultObj.getDouble("latitude")
                val lng = resultObj.getDouble("longitude")
                val cityName = resultObj.getString("name")
                val country = resultObj.optString("country", "")
                val locationName = if (country.isNotBlank()) "$cityName, $country" else cityName

                // 2. Fetch current weather
                val weatherUrl = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lat&longitude=$lng&current_weather=true"
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
            } catch (e: java.net.UnknownHostException) {
                weatherInfo = WeatherInfo(
                    temperature = 0.0, windspeed = 0.0, condition = "", locationName = "",
                    isError = true, errorMessage = "No internet connection. Please check your network and try again."
                )
            } catch (e: java.net.SocketTimeoutException) {
                weatherInfo = WeatherInfo(
                    temperature = 0.0, windspeed = 0.0, condition = "", locationName = "",
                    isError = true, errorMessage = "Request timed out. Please try again."
                )
            } catch (e: Exception) {
                weatherInfo = WeatherInfo(
                    temperature = 0.0, windspeed = 0.0, condition = "", locationName = "",
                    isError = true, errorMessage = "Could not load weather data. Please try again."
                )
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun performGetRequest(urlString: String): String = withContext(Dispatchers.IO) {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("Accept", "application/json")

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                throw Exception("HTTP $responseCode from server")
            }
        } finally {
            conn.disconnect()
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
