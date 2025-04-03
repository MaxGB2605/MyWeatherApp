package com.example.myweatherapp2.dependency_injection

import com.example.myweatherapp2.storage.SharedPreferencesManager
import org.koin.dsl.module

val storageModule = module {
    single { SharedPreferencesManager(context = get(), gson = get()) }
}