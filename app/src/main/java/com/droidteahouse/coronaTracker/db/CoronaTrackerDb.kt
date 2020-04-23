/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.droidteahouse.coronaTracker.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import android.graphics.Typeface.createFromAsset
import androidx.room.TypeConverters
import com.droidteahouse.coronaTracker.vo.ApiResponse
import com.droidteahouse.coronaTracker.vo.Area
import java.io.IOException
import java.io.InputStream

/**
 * Database schema used by the corona tracker
 */
@Database(
        entities = [ApiResponse::class, Area::class],
        version = 1,
        exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CoronaTrackerDb : RoomDatabase() {
    companion object {
        fun create(context: Context): CoronaTrackerDb {
            val databaseBuilder = Room.databaseBuilder(context, CoronaTrackerDb::class.java, "corona_init.db")

            return databaseBuilder
                    .fallbackToDestructiveMigration()
                    .build()
        }
    }

    abstract fun dao(): CoronaTrackerDao

}