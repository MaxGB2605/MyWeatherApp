package com.example.myweatherapp2.data

data class LiveDataEvent<out T>(private val content: T) {

    private var hasBeenHandled = false


    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }


}
