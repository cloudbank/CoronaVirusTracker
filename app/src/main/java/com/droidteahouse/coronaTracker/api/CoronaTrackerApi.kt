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

package com.droidteahouse.coronaTracker.api

import android.util.Log
import com.droidteahouse.coronaTracker.vo.Area
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET


/**
 * API communication setup
 */
interface CoronaTrackerApi {
    @GET("/wiki/2019%E2%80%9320_coronavirus_pandemic")
    suspend fun scrape(): Response<String>


    data class CoronaTrackerResponse(val data: Area)

    companion object {
        private const val BASE_URL = "https://en.wikipedia.org"
        fun create(): CoronaTrackerApi = create(HttpUrl.parse(BASE_URL)!!)
        fun create(httpUrl: HttpUrl): CoronaTrackerApi {
            val logger = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger {
                Log.d("API", it)
            })
            logger.level = HttpLoggingInterceptor.Level.BASIC

            val client = OkHttpClient.Builder()
                    .addInterceptor(logger)
                    .build()

            var gson: Gson? = GsonBuilder().serializeNulls().create()
            return Retrofit.Builder()
                    .baseUrl(httpUrl)
                    .client(client)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build()
                    .create(CoronaTrackerApi::class.java)
        }
    }


}