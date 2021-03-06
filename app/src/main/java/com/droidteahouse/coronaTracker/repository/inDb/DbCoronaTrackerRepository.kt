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

import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.paging.toLiveData
import androidx.room.withTransaction
import com.droidteahouse.coronaTracker.api.CoronaTrackerApi
import com.droidteahouse.coronaTracker.db.CoronaTrackerDb
import com.droidteahouse.coronaTracker.repository.CoronaTrackerRepository
import com.droidteahouse.coronaTracker.repository.Listing
import com.droidteahouse.coronaTracker.repository.NetworkState
import com.droidteahouse.coronaTracker.vo.ApiResponse
import com.droidteahouse.coronaTracker.vo.Area
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Repository implementation that uses a database PagedList + a boundary callback to return a
 * listing that loads in pages.
 */
class DbCoronaTrackerRepository(
        val db: CoronaTrackerDb,
        private val coronaTrackerApi: CoronaTrackerApi,
        private var boundaryCallback: CoronaTrackerBoundaryCallback
) : CoronaTrackerRepository {

    init {
        boundaryCallback.handleResponse = this::insertResultIntoDb
    }

    companion object {

        private val TAG = DbCoronaTrackerRepository::class.java.canonicalName
    }

    /**
     * Inserts the response into the database while also assigning position indices to items.
     */
    private suspend fun updateResult(body: ApiResponse?) {
        var num = 0
        body?.let { it ->
            //@Transaction
            num = db.dao().updateAll(it)
            Log.d(TAG, "Update on ${num} rows successful")
        }
    }

    /**
     * Inserts the response into the database while also assigning position indices to items.
     */
    private suspend fun insertResultIntoDb(body: ApiResponse?) {
        body?.let { it ->
            db.withTransaction {
                db.dao().insertWorld(it)
                db.dao().insert(it.areas)
            }
        }
    }

    /**
     * When refresh is called, we simply run a fresh network request and when it arrives, clear
     * the database table and insert all new items in a transaction.
     * <p>
     * Since the PagedList already uses a database bound data source, it will automatically be
     * updated after the database transaction is finished.
     */
    @MainThread
    private fun refresh(mainScope: CoroutineScope): LiveData<NetworkState> {
        val networkState = MutableLiveData<NetworkState>()
        networkState.value = NetworkState.LOADING
        mainScope.launch {
            try {
                val response = CoronaTrackerApi.safeApiCall(null, networkState) { coronaTrackerApi.scrape() }

                if (response != null) {
                    if (response.isSuccessful) {
                        withContext(Dispatchers.IO) { updateResult(response.body()?.let { it1 -> ApiResponse.fromString(it1) }) }
                        networkState.value = (NetworkState.LOADED)
                    } else {
                        networkState.value = (NetworkState.error(response.errorBody().toString()))
                    }
                }
            } catch (e: Exception) {
                networkState.value = (NetworkState.error(e.message))
            }
        }
        return networkState
    }

    /**
     * Returns a Listing for the given data
     *
     */
    @MainThread
    override suspend fun areasOfCoronaTracker(pageSize: Int, mainScope: CoroutineScope): Listing<Area> {
        // create a boundary callback which will observe when the user reaches to the edges of
        // the list and update the database with extra data.
        // we are using a mutable live data to trigger refresh requests which eventually calls
        // refresh method and gets a new live data. Each refresh request by the user becomes a newly
        // dispatched data in refreshTrigger
        val refreshTrigger = MutableLiveData<Unit>()
        val refreshState = refreshTrigger.switchMap {
            refresh(mainScope)
        }

        // We use toLiveData Kotlin extension function here, you could also use LivePagedListBuilder
        val livePagedList =
                db.dao().areas().toLiveData(
                        pageSize = pageSize,
                        boundaryCallback = boundaryCallback)

        boundaryCallback.scope = mainScope

        return Listing(
                pagedList = livePagedList,
                networkState = boundaryCallback.networkState,
                retry = {
                    boundaryCallback.helper.retryAllFailed()
                },
                refresh = {
                    refreshTrigger.value = null
                },
                refreshState = refreshState
        )
    }


    override fun worldData(): LiveData<ApiResponse> {
        return db.dao().world()
    }


}

