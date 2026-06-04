package com.example.wellnesstracker.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object JsonUtils {
    val gson = Gson()

    fun <T> toJson(obj: T): String = gson.toJson(obj)

    inline fun <reified T> fromJson(json: String?): T {
        val type = object : TypeToken<T>() {}.type
        return gson.fromJson(json, type)
    }
}
