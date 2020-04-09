package com.droidteahouse.coronaTracker.db

import androidx.room.TypeConverter
import com.droidteahouse.coronaTracker.vo.Area
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    @TypeConverter
    fun fromAreaList(value: List<Area>): String {
        val gson = Gson()
        val type = object : TypeToken<List<Area>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toAreaList(value: String): List<Area> {
        val gson = Gson()
        val type = object : TypeToken<List<Area>>() {}.type
        return gson.fromJson(value, type)
    }

}

