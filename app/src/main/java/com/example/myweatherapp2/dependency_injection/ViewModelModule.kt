package com.example.myweatherapp2.dependency_injection

import com.example.myweatherapp2.fragments.home.HomeViewModel
import com.example.myweatherapp2.fragments.location.LocationViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { HomeViewModel(weatherDataRepository = get()) }
    viewModel { LocationViewModel(weatherDataRepository = get()) }
}