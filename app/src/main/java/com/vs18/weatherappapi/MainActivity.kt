package com.vs18.weatherappapi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.bumptech.glide.integration.compose.GlideImage
import com.vs18.weatherappapi.ui.theme.WeatherAppAPITheme
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeatherAppAPITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121212)
                ) {
                    WeatherScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun WeatherScreen() {
    var city by remember { mutableStateOf("Київ") }
    var currentWeather by remember { mutableStateOf<CurrentWeatherResponse?>(null) }
    var forecast by remember { mutableStateOf<ForecastResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "🌤️ Погода",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00b7eb),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("Місто") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (city.isBlank()) return@Button
                isLoading = true
                error = null
                scope.launch {
                    val currentResult = safeWeatherCall { WeatherApiModule.weatherService.getCurrentWeather(city) }
                    val forecastResult = safeWeatherCall { WeatherApiModule.weatherService.getForecast(city) }

                    currentResult.onSuccess { currentWeather = it }
                    currentResult.onFailure { error = "Помилка: ${it.message}" }

                    forecastResult.onSuccess { forecast = it }
                    forecastResult.onFailure { error = "Помилка прогнозу: ${it.message}" }

                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43a047))
        ) {
            Text("🔍 Отримати погоду", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        error?.let {
            Text(it, color = Color.Red, modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        currentWeather?.let { weather ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1e1e1e)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        "${weather.name}, ${weather.sys.country}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        if (isLoading) {
                            CircularProgressIndicator()
                        }

                        GlideImage(
                            model = getWeatherIconUrl(weather.weather[0].icon),
                            contentDescription = "Іконка погоди",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Text(
                        "${weather.main.temp.toInt()}°C",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        weather.weather[0].description.replaceFirstChar { it.uppercase() },
                        fontSize = 20.sp,
                        color = Color(0xFFaaaaaa)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceAround,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        WeatherDetail("Відчувається", "${weather.main.feels_like.toInt()}°C")
                        WeatherDetail("Вологість", "${weather.main.humidity}%")
                        WeatherDetail("Вітер", "${weather.wind.speed} м/с")
                        WeatherDetail("Тиск", "${weather.main.pressure} гПа")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        forecast?.let { f ->
            Text(
                "Прогноз на 5 днів",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00b7eb)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(f.list.chunked(8).take(5)) { dayItems ->
                    val item = dayItems[0]
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF252526)),
                        modifier = Modifier.width(140.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                SimpleDateFormat("EEE, d MMM", Locale("uk")).format(Date(item.dt * 1000)),
                                fontSize = 14.sp,
                                color = Color.White
                            )

                            GlideImage(
                                model = getWeatherIconUrl(item.weather[0].icon),
                                contentDescription = null,
                                modifier = Modifier.size(60.dp)
                            )

                            Text(
                                "${item.main.temp.toInt()}°",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Text(
                                item.weather[0].description,
                                fontSize = 12.sp,
                                color = Color(0xFFaaaaaa)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherDetail(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 14.sp, color = Color(0xFFaaaaaa))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}