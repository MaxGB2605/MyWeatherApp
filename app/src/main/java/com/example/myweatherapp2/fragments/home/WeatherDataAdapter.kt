package com.example.myweatherapp2.fragments.home

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.myweatherapp2.data.CurrentLocation
import com.example.myweatherapp2.data.CurrentWeather
import com.example.myweatherapp2.data.Forecast
import com.example.myweatherapp2.data.WeatherData
import com.example.myweatherapp2.databinding.ItemContainerCurrentLocationBinding
import com.example.myweatherapp2.databinding.ItemContainerCurrentWeatherBinding
import com.example.myweatherapp2.databinding.ItemContainerForecastBinding

class WeatherDataAdapter(
    private val onLocationClicked: () -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    private companion object {
        const val INDEX_CURRENT_LOCATION = 0
        const val INDEX_CURRENT_WEATHER = 1
        const val INDEX_FORECAST = 2
    }


    private val weatherData = mutableListOf<WeatherData>()


    fun setCurrentLocation(currentLocation: CurrentLocation) {
        //Added on May 17, 2025 to update location on swipe
        val index = weatherData.indexOfFirst { it is CurrentLocation }

        if (index != -1) {
            weatherData[index] = currentLocation
            notifyItemChanged(index)
        } else {
            weatherData.add(0, currentLocation)
            notifyItemInserted(0)
        }


// ------------Original----------------
//        if (weatherData.isEmpty()) {
//            weatherData.add(INDEX_CURRENT_LOCATION, currentLocation)
//            notifyItemInserted(INDEX_CURRENT_LOCATION)
//        } else {
//            weatherData[INDEX_CURRENT_LOCATION] = currentLocation
//            notifyItemChanged(INDEX_CURRENT_LOCATION)
//        }
    }


    fun setCurrentWeather(currentWeather: CurrentWeather) {
        if (weatherData.getOrNull(INDEX_CURRENT_WEATHER) != null) {
            weatherData[INDEX_CURRENT_WEATHER] = currentWeather
            notifyItemChanged(INDEX_CURRENT_WEATHER)
        } else {
            weatherData.add(INDEX_CURRENT_WEATHER, currentWeather)
            notifyItemInserted(INDEX_CURRENT_WEATHER)
        }
    }


    fun setForecastData(forecast: List<Forecast>) {
        //Added on May 17, 2025 to prevent crash
        // Ensure we don't crash when inserting at INDEX_FORECAST
        if (weatherData.size < INDEX_FORECAST) {
            weatherData.addAll(forecast)
            notifyItemRangeInserted(weatherData.size - forecast.size, forecast.size)
            return
        }

        // Count how many forecasts exist and remove them
        val oldForecastCount = weatherData.count { it is Forecast }
        weatherData.removeAll { it is Forecast }
        notifyItemRangeRemoved(INDEX_FORECAST, oldForecastCount)

        // Insert new forecast data
        weatherData.addAll(INDEX_FORECAST, forecast)
        notifyItemRangeInserted(INDEX_FORECAST, forecast.size)

//original  -----------------
//        weatherData.removeAll { it is Forecast }
//        notifyItemRangeRemoved(INDEX_FORECAST, weatherData.size)
//        weatherData.addAll(INDEX_FORECAST, forecast)
//        notifyItemRangeChanged(INDEX_FORECAST, weatherData.size)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            INDEX_CURRENT_LOCATION -> CurrentLocationViewHolder(
                ItemContainerCurrentLocationBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )

            INDEX_FORECAST -> ForecastViewHolder(
                ItemContainerForecastBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )

            else -> CurrentWeatherViewHolder(
                ItemContainerCurrentWeatherBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CurrentLocationViewHolder -> holder.bind(weatherData[position] as CurrentLocation)
            is CurrentWeatherViewHolder -> holder.bind(weatherData[position] as CurrentWeather)
            is ForecastViewHolder -> holder.bind(weatherData[position] as Forecast)
        }

    }

    override fun getItemCount(): Int {
        return weatherData.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (weatherData[position]) {
            is CurrentLocation -> INDEX_CURRENT_LOCATION
            is CurrentWeather -> INDEX_CURRENT_WEATHER
            is Forecast -> INDEX_FORECAST
        }
    }

    inner class CurrentLocationViewHolder(
        private val binding: ItemContainerCurrentLocationBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(currentLocation: CurrentLocation) {
            with(binding) {

                //added May 10.2025 to update date on swipe refresh
                val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                val currentTime = timeFormat.format(java.util.Date())
                val dateFormat =
                    java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault())
                val currentDate = dateFormat.format(java.util.Date())
                textCurrentDate.text = "$currentDate $currentTime"



                textCurrentLocation.text = currentLocation.location
                imageCurrentLocation.setOnClickListener { onLocationClicked() }
                textCurrentLocation.setOnClickListener { onLocationClicked() }
            }
        }
    }

    inner class CurrentWeatherViewHolder(
        private val binding: ItemContainerCurrentWeatherBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(currentWeather: CurrentWeather) {


            with(binding) {
                imageIcon.load("https:${currentWeather.icon}") { crossfade(true) }
                //textTemperature.text = String.format("%s\u00B0C", currentWeather.temperature)

                Log.d(
                    "WeatherApp",
                    "Formatted Temp: ${String.format("%s\u00B0C", currentWeather.temperature)}"
                )
                textTemperature.text = String.format("%s\u00B0C", currentWeather.temperature)


                textWind.text = String.format("%s km/h", currentWeather.wind)
                textHumidity.text = String.format("%s%%", currentWeather.humidity)
                textChanceOfRain.text = String.format("%s%%", currentWeather.chanceOfRain)
            }
        }
    }

    inner class ForecastViewHolder(
        private val binding: ItemContainerForecastBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(forecast: Forecast) {
            with(binding) {
                textTime.text = forecast.time
                //textTemperature.text = String.format("%s\u00B0C", forecast.temperature)
                Log.d(
                    "WeatherApp",
                    "Formatted Temp: ${String.format("%s\u00B0C", forecast.temperature)}"
                )
                textTemperature.text = String.format("%s\u00B0C", forecast.temperature)

                textFeelsLikeTemperature.text =
                    String.format("%s\u00B0C", forecast.feelsLikeTemperature)
                imageIcon.load("https:${forecast.icon}") { crossfade(true) }


            }


        }
    }


}