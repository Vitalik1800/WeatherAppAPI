package com.vs18.weatherappapi

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okio.IOException
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

object WeatherApiModule {

    private const val API_KEY = "fdb3f630dd601cb993f8ebaa0fb36160"

    private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val original = chain.request()
            val url = original.url.newBuilder()
                .addQueryParameter("appid", API_KEY)
                .addQueryParameter("units", "metric")
                .addQueryParameter("lang", "ua")
                .build()
            chain.proceed(original.newBuilder().url(url).build())
        }
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val weatherService: WeatherService = retrofit.create(WeatherService::class.java)
}

interface WeatherService {

    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("q") city: String
    ): CurrentWeatherResponse

    @GET("weather")
    suspend fun getCurrentWeatherByCoords(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): CurrentWeatherResponse

    @GET("forecast")
    suspend fun getForecast(
        @Query("q") city: String
    ): ForecastResponse
}

data class CurrentWeatherResponse(
    val name: String,
    val main: Main,
    val weather: List<Weather>,
    val wind: Wind,
    val sys: Sys,
    val coord: Coord
)

data class Main(
    val temp: Double,
    val feels_like: Double,
    val humidity: Int,
    val pressure: Int
)

data class Weather(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

data class Wind(
    val speed: Double,
    val deg: Int
)

data class Sys(
    val country: String,
    val sunrise: Long,
    val sunset: Long
)

data class Coord(
    val lon: Double,
    val lat: Double
)

data class ForecastResponse(
    val list: List<ForecastItem>,
    val city: CityInfo
)

data class ForecastItem(
    val dt: Long,
    val main: Main,
    val weather: List<Weather>,
    val wind: Wind,
    val dt_txt: String
)

data class CityInfo(
    val name: String,
    val country: String
)

suspend fun <T> safeWeatherCall(call: suspend () -> T): Result<T> {
    return try {
        Result.success(call())
    } catch (e: HttpException) {
        Result.failure(Exception("Помилка HTTP ${e.code()}: ${e.message()}"))
    } catch (_: IOException) {
        Result.failure(Exception("Немає інтернету або сервер недоступний"))
    } catch (e: Exception){
        Result.failure(Exception("Помилка: ${e.message}"))
    }
}

fun getWeatherIconUrl(iconCode: String): String {
    return "https://openweathermap.org/img/wn/${iconCode}@4x.png"
}