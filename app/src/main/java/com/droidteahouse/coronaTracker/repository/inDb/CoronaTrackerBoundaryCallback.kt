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

import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import androidx.paging.PagingRequestHelper
import com.droidteahouse.coronaTracker.api.CoronaTrackerApi
import com.droidteahouse.coronaTracker.repository.NetworkState
import com.droidteahouse.coronaTracker.util.createStatusLiveData
import com.droidteahouse.coronaTracker.vo.ApiResponse
import com.droidteahouse.coronaTracker.vo.Area
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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
        private val ioExecutor: Executor,
        private val networkPageSize: Int)
    : PagedList.BoundaryCallback<Area>() {
    lateinit var handleResponse: KFunction1<@ParameterName(name = "body") ApiResponse?, Unit>
    val helper = PagingRequestHelper(ioExecutor)
    var networkState = helper.createStatusLiveData() as MutableLiveData

    /**
     * Database returned 0 items. We should query the backend for more items.
     */
    @MainThread
    override fun onZeroItemsLoaded() {
        helper.runIfNotRunning(PagingRequestHelper.RequestType.INITIAL) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val api = async(Dispatchers.IO) { CoronaTrackerApi.safeApiCall(it, networkState) { webservice.scrape() } }
                    val response = api.await()
                    if (response != null) {
                        if (response.isSuccessful) launch(Dispatchers.IO) { insertItemsIntoDb(response, it) } else it.recordFailure(Throwable(response.errorBody().toString()))
                    }
                } catch (e: Exception) {
                    networkState.value = (NetworkState.error(e.message ?: "unknown err"))
                    it.recordFailure(e)
                }

            }
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


    /**
     * every time it gets new items, boundary callback simply inserts them into the database and
     * paging library takes care of refreshing the list if necessary.
     */
    private fun insertItemsIntoDb(
            response: Response<String>,
            it: PagingRequestHelper.Request.Callback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleResponse?.invoke(response.body()?.let { it1 -> ApiResponse.fromString(it1) })
                it.recordSuccess()
            } catch (e: Exception) {
                it.recordFailure(e)
            }
        }
    }


}