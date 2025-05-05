package com.example.myweatherapp2.fragments.home

import android.location.Geocoder
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myweatherapp2.data.CurrentLocation
import com.example.myweatherapp2.data.CurrentWeather
import com.example.myweatherapp2.data.Forecast
import com.example.myweatherapp2.data.LiveDataEvent
import com.example.myweatherapp2.network.repository.WeatherDataRepository
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeViewModel(
    private val weatherDataRepository: WeatherDataRepository,
) : ViewModel() {


    //region Current Location


    private val _currentLocation = MutableLiveData<LiveDataEvent<CurrentLocationDataState>>()
    val currentLocation: LiveData<LiveDataEvent<CurrentLocationDataState>> get() = _currentLocation


    fun getCurrentLocation(
        fusedLocationProviderClient: FusedLocationProviderClient,
        geocoder: Geocoder,
    ) {
        viewModelScope.launch {
            emitCurrentLocationUiState(isLoading = true)
            weatherDataRepository.getCurrentLocation(
                fusedLocationProviderClient = fusedLocationProviderClient,
                onSuccess = { currentLocation ->
                    updateAddressText(currentLocation, geocoder)
                },
                onFailure = {
                    emitCurrentLocationUiState(error = "Unable to fetch current location")
                }
            )
        }
    }

    private fun updateAddressText(currentLocation: CurrentLocation, geocoder: Geocoder) {
        viewModelScope.launch {
            runCatching {
                weatherDataRepository.updateAddressText(currentLocation, geocoder)
            }.onSuccess { location ->
                emitCurrentLocationUiState(currentLocation = location)
            }.onFailure {
                emitCurrentLocationUiState(
                    currentLocation = currentLocation.copy(
                        location = "N/A"
                    )
                )
            }
        }
    }

    private fun emitCurrentLocationUiState(
        isLoading: Boolean = false,
        currentLocation: CurrentLocation? = null,
        error: String? = null,
    ) {
        val currentLocationDataState = CurrentLocationDataState(isLoading, currentLocation, error)
        _currentLocation.value = LiveDataEvent(currentLocationDataState)
    }

    data class CurrentLocationDataState(
        val isLoading: Boolean,
        val currentLocation: CurrentLocation?,
        val error: String?,
    )
    //endregion


    //region Weather Data
    private val _weatherData = MutableLiveData<LiveDataEvent<WeatherDataState>>()
    val weatherData: LiveData<LiveDataEvent<WeatherDataState>> get() = _weatherData


    fun getWeatherData(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            emitWeatherDataUiState(isLoading = true)

            Log.d("HomeViewModel", "Calling repository.getWeatherData")


            val weatherData = weatherDataRepository.getWeatherData(latitude, longitude)

            //weatherDataRepository.getWeatherData(latitude, longitude)?.let { weatherData ->

            if (weatherData != null) {
                Log.d("HomeViewModel", "Weather data received")

                //added 05.05.2025
                // Move these outside of the UI state call
                val now = Calendar.getInstance().time
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

                val forecast = weatherData.forecast.forecastDay
                    .take(3) // take next 3 days
                    .flatMap { it.hour }
                    .filter { hourData ->
                        val forecastTime = inputFormat.parse(hourData.time)
                        forecastTime != null && forecastTime.after(now)
                    }
                    .map {
                        Forecast(
                            time = getForecastTime(it.time),
                            temperature = it.temperature,
                            feelsLikeTemperature = it.feelsLikeTemperature,
                            icon = it.condition.icon
                        )
                    }

                emitWeatherDataUiState(
                    currentWeather = CurrentWeather(
                        icon = weatherData.current.condition.icon,
                        temperature = weatherData.current.temperature,
                        wind = weatherData.current.wind,
                        humidity = weatherData.current.humidity,
                        chanceOfRain = weatherData.forecast.forecastDay.first().day.chanceOfRain
                    ),
                    //added 05.05.2025
                    forecast = forecast


//                    forecast = weatherData.forecast.forecastDay.first().hour.map {
//                        Forecast(
//                            time = getForecastTime(it.time),
//                            temperature = it.temperature,
//                            feelsLikeTemperature = it.feelsLikeTemperature,
//                            icon = it.condition.icon
//                        )
//                    }
                )
            } else {
                Log.d("HomeViewModel", "Failed to fetch weather data")
                emitWeatherDataUiState(error = "Unable to fetch weather data")
            }
            //?: emitWeatherDataUiState(error = "Unable to fetch weather data")
        }
    }


    //added to he;lp with getting time for the forecast past 23:00
    private fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Calendar.getInstance().time)
    }


    private fun emitWeatherDataUiState(
        isLoading: Boolean = false,
        currentWeather: CurrentWeather? = null,
        forecast: List<Forecast>? = null,
        error: String? = null,
    ) {
        val weatherDataState = WeatherDataState(isLoading, currentWeather, forecast, error)
        _weatherData.value = LiveDataEvent(weatherDataState)
    }

    data class WeatherDataState(
        val isLoading: Boolean,
        val currentWeather: CurrentWeather?,
        val forecast: List<Forecast>?,
        val error: String?,
    )


    private fun getForecastTime(dateTime: String): String {

        val pattern = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val date = pattern.parse(dateTime) ?: return dateTime
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    }
    //endregion
}