package com.example.myweatherapp2.fragments.home


import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.example.myweatherapp2.R
import com.example.myweatherapp2.data.CurrentLocation
import com.example.myweatherapp2.databinding.FragmentHomeBinding
import com.example.myweatherapp2.storage.SharedPreferencesManager
import com.google.android.gms.location.LocationServices
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel


class HomeFragment : Fragment() {


    companion object {
        const val REQUEST_KEY_MANUAL_LOCATION_SEARCH = "manualLocationSearch"
        const val KEY_LOCATION_TEXT = "locationText"
        const val KEY_LATITUDE = "latitude"
        const val KEY_LONGITUDE = "longitude"
    }


    private var _binding: FragmentHomeBinding? = null
    private val binding get() = requireNotNull(_binding)


    private val homeViewModel: HomeViewModel by viewModel()
    private val fusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireContext())
    }


    private val geocoder by lazy { Geocoder(requireContext()) }


    private val weatherDataAdapter = WeatherDataAdapter(
        onLocationClicked = { showLocationOptions() }
    )


    private val sharedPreferencesManager: SharedPreferencesManager by inject()


    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getCurrentLocation()
        } else {
            Toast.makeText(requireContext(), "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }


    private var isInitialLocationSet: Boolean = false


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        setWeatherDataAdapter()
        setObservers()
        setListeners()

        //Added on May 17, 2025 to update location on swipe
        setLocationListeners()



        if (!isInitialLocationSet) {
            setCurrentLocation(currentLocation = sharedPreferencesManager.getCurrentLocation())
            isInitialLocationSet = true
        }
    }


    private fun setListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener {

            val currentLocation = sharedPreferencesManager.getCurrentLocation()
            setCurrentLocation(currentLocation)
            currentLocation?.let {
                getWeatherData(it) // <--- Force weather update here
            }

            setCurrentLocation(sharedPreferencesManager.getCurrentLocation())
        }
    }

    //Added on May 17, 2025 to update location on swipe
    private fun setLocationListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            // Check if location permission is granted
            if (isLocationPermissionGranted()) {
                // Fetch the current location
                getCurrentLocation()
            } else {
                // Request location permission
                requestLocationPermission()
            }
        }
    }

    private fun setObservers() {
        with(homeViewModel) {
            currentLocation.observe(viewLifecycleOwner) {
                val currentLocationDataState = it.getContentIfNotHandled() ?: return@observe
                if (currentLocationDataState.isLoading) {
                    showLoading()
                }
                currentLocationDataState.currentLocation?.let { currentLocation ->
                    showLoading()
                    sharedPreferencesManager.saveCurrentLocation(currentLocation)
                    setCurrentLocation(currentLocation)
                }
                currentLocationDataState.error?.let { error ->
                    hideLoading()
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                }
            }
            weatherData.observe(viewLifecycleOwner) {
                val weatherDataState = it.getContentIfNotHandled() ?: return@observe

                binding.swipeRefreshLayout.isRefreshing = weatherDataState.isLoading

                weatherDataState.currentWeather?.let { currentWeather ->
                    weatherDataAdapter.setCurrentWeather(currentWeather)
                }
                weatherDataState.forecast?.let { forecasts ->
                    weatherDataAdapter.setForecastData(forecasts)
                }

                //Added on May 17, 2025 to hide spinner after weather data is loaded
                if (!weatherDataState.isLoading) {
                    hideLoading()
                }


                weatherDataState.error?.let { error ->
                    hideLoading()
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                }
            }


        }
    }

    private fun setWeatherDataAdapter() {
        binding.weatherDataRecyclerView.itemAnimator = null
        binding.weatherDataRecyclerView.adapter = weatherDataAdapter
    }

    private fun setCurrentLocation(currentLocation: CurrentLocation? = null) {

        weatherDataAdapter.setCurrentLocation(currentLocation ?: CurrentLocation())
        currentLocation?.let { getWeatherData(currentLocation = it) }
    }


    private fun getCurrentLocation() {
        homeViewModel.getCurrentLocation(fusedLocationProviderClient, geocoder)
    }

    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun proceedWithCurrentLocation() {
        if (isLocationPermissionGranted()) {
            getCurrentLocation()
        } else {
            requestLocationPermission()
        }
    }

    private fun showLocationOptions() {
        val options = arrayOf("Current Location", "Search Manually")
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Choose Location Method")
            setItems(options) { _, which ->
                when (which) {
                    0 -> proceedWithCurrentLocation()
                    1 -> startManualLocationSearch()
                }
            }
            show()
        }
    }

    private fun showLoading() {
        with(binding) {
            weatherDataRecyclerView.visibility = View.VISIBLE
            swipeRefreshLayout.isEnabled = true
            swipeRefreshLayout.isRefreshing = true
        }
    }


//-------original location-----------------
//    private fun hideLoading() {
//        with(binding) {
//            weatherDataRecyclerView.visibility = View.VISIBLE
//            swipeRefreshLayout.isEnabled = true
//            swipeRefreshLayout.isRefreshing = false
//        }
//    }


    private fun startManualLocationSearch() {
        startListeningManualLocationSelection()
        findNavController().navigate(R.id.action_home_fragment_to_location_fragment)
    }


    private fun startListeningManualLocationSelection() {
        setFragmentResultListener(REQUEST_KEY_MANUAL_LOCATION_SEARCH) { _, bundle ->
            stopListeningManualLocationSelection()
            val currentLocation = CurrentLocation(
                location = bundle.getString(KEY_LOCATION_TEXT) ?: "N/A",
                latitude = bundle.getDouble(KEY_LATITUDE),
                longitude = bundle.getDouble(KEY_LONGITUDE)
            )
            sharedPreferencesManager.saveCurrentLocation(currentLocation)
            setCurrentLocation(currentLocation)
        }
    }


    private fun stopListeningManualLocationSelection() {
        clearFragmentResultListener(REQUEST_KEY_MANUAL_LOCATION_SEARCH)
    }


    private fun getWeatherData(currentLocation: CurrentLocation) {
        if (currentLocation.latitude != null && currentLocation.longitude != null) {
            homeViewModel.getWeatherData(
                latitude = currentLocation.latitude,
                longitude = currentLocation.longitude
            )
        }
    }

    // Moved it to keep spinner till data is loaded
    private fun hideLoading() {
        with(binding) {
            weatherDataRecyclerView.visibility = View.VISIBLE
            swipeRefreshLayout.isEnabled = true
            swipeRefreshLayout.isRefreshing = false
        }
    }

}