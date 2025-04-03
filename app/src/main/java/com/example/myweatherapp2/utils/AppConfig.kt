package com.example.myweatherapp2.utils

import android.app.Application
import com.example.myweatherapp2.dependency_injection.repositoryModule
import com.example.myweatherapp2.dependency_injection.viewModelModule
import org.koin.core.context.startKoin

class AppConfig : Application() {

    override fun onCreate() {

        super.onCreate()
        startKoin {
            modules(listOf(repositoryModule, viewModelModule))
        }

    }


}