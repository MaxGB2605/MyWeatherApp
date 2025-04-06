package com.example.myweatherapp2.dependency_injection

import com.example.myweatherapp2.network.repository.WeatherDataRepository
import org.koin.dsl.module

val repositoryModule = module {
    single { WeatherDataRepository(weatherAPI = get()) }
}