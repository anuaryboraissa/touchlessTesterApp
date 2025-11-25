package com.example.touchlesstester

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FingersDataConverter {

    private val gson = Gson()

    // Convert List<ResponseFingersData> -> JSON String
    fun toJson(list: List<ResponseFingersData>): String {
        return gson.toJson(list)
    }

    // Convert JSON String -> List<ResponseFingersData>
    fun fromJson(json: String): List<ResponseFingersData> {
        val type = object : TypeToken<List<ResponseFingersData>>() {}.type
        return gson.fromJson(json, type)
    }
}
