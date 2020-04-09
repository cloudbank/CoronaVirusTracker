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

package com.droidteahouse.coronaTracker.repository.inDb

import androidx.paging.PagedList
import androidx.paging.PagingRequestHelper
import androidx.annotation.MainThread
import com.droidteahouse.coronaTracker.MyApplication
import com.droidteahouse.coronaTracker.vo.ApiResponse
import com.droidteahouse.coronaTracker.api.CoronaTrackerApi
import com.droidteahouse.coronaTracker.util.createStatusLiveData
import com.droidteahouse.coronaTracker.vo.Area
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executor
import kotlin.reflect.KFunction1

/**
 * This boundary callback gets notified when user reaches to the edges of the list such that the
 * database cannot provide any more data.
 * <p>
 * The boundary callback might be called multiple times for the same direction so it does its own
 * rate limiting using the PagingRequestHelper class.
 */
class CoronaTrackerBoundaryCallback(
        private val webservice: CoronaTrackerApi,
        private val handleResponse: KFunction1<@ParameterName(name = "body") ApiResponse?, Unit>,
        private val insertFromFile: KFunction1<@ParameterName(name = "body") ApiResponse?, Unit>,
        private val ioExecutor: Executor,
        private val networkPageSize: Int)
    : PagedList.BoundaryCallback<Area>() {

    val helper = PagingRequestHelper(ioExecutor)
    val networkState = helper.createStatusLiveData()

    /**
     * Database returned 0 items. We should query the backend for more items.
     */
    @MainThread
    override fun onZeroItemsLoaded() {
        helper.runIfNotRunning(PagingRequestHelper.RequestType.INITIAL) {
            webservice.scrape()
                    .enqueue(createWebserviceCallback(it))
        }
    }

    /**
     *
     */
    @MainThread
    override fun onItemAtFrontLoaded(itemAtFront: Area) {
    }

    /**
     * User reached to the end of the list.
     */
    @MainThread
    override fun onItemAtEndLoaded(itemAtEnd: Area) {
    }

    private fun insertFromFile() {
        ioExecutor.execute {
            insertFromFile(ApiResponse.fromFile(MyApplication.instance))
        }
    }

    /**
     * every time it gets new items, boundary callback simply inserts them into the database and
     * paging library takes care of refreshing the list if necessary.
     */
    private fun insertItemsIntoDb(
            response: Response<String>,
            it: PagingRequestHelper.Request.Callback) {
        ioExecutor.execute {
            handleResponse(ApiResponse.fromString(response.body().toString()))
            it.recordSuccess()
        }
    }

    private fun createWebserviceCallback(it: PagingRequestHelper.Request.Callback)
            : Callback<String> {
        return object : Callback<String> {
            override fun onFailure(
                    call: Call<String>,
                    t: Throwable) {
                it.recordFailure(t)
            }

            override fun onResponse(
                    call: Call<String>,
                    response: Response<String>) {
                insertItemsIntoDb(response, it)
            }
        }
    }
}