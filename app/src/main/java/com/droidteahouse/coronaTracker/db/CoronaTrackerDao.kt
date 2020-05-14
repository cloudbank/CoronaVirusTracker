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

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*
import com.droidteahouse.coronaTracker.vo.ApiResponse
import com.droidteahouse.coronaTracker.vo.Area


@Dao
interface CoronaTrackerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(areas: List<Area>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorld(apiResponse: ApiResponse)

    @Transaction
    suspend fun updateAll(apiResponse: ApiResponse): Int {
        updateWorld(apiResponse)
        return updateAreas(apiResponse.areas)
    }

    @Update
    suspend fun updateWorld(apiResonse: ApiResponse): Int
    @Update
    suspend fun updateAreas(areas: List<Area>): Int

    @Query("SELECT * FROM coronatracker")
    fun world(): LiveData<ApiResponse>

    @Query("SELECT * FROM areas  ORDER BY totalConfirmedInt DESC")
    fun areas(): DataSource.Factory<Int, Area>

}