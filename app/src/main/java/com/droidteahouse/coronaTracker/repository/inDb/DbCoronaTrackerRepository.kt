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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.paging.toLiveData
import com.droidteahouse.coronaTracker.api.CoronaTrackerApi
import com.droidteahouse.coronaTracker.db.CoronaTrackerDb
import com.droidteahouse.coronaTracker.repository.Listing
import com.droidteahouse.coronaTracker.repository.NetworkState
import com.droidteahouse.coronaTracker.repository.CoronaTrackerRepository
import com.droidteahouse.coronaTracker.vo.ApiResponse
import com.droidteahouse.coronaTracker.vo.Area
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executor

/**
 * Repository implementation that uses a database PagedList + a boundary callback to return a
 * listing that loads in pages.
 */
class DbCoronaTrackerRepository(
        val db: CoronaTrackerDb,
        private val coronaTrackerApi: CoronaTrackerApi,
        private val ioExecutor: Executor,
        private val networkPageSize: Int = DEFAULT_NETWORK_PAGE_SIZE) : CoronaTrackerRepository {
    companion object {
        private const val DEFAULT_NETWORK_PAGE_SIZE = 10
    }

    /**
     * Inserts the response into the database while also assigning position indices to items.
     */
    private fun updateResult(body: ApiResponse?) {
        body?.let { it ->
            db.runInTransaction {
                //this is going to be an update
                db.dao().updateWorld(it)
                db.dao().updateAreas(it.areas)
            }
        }
    }

    /**
     * Inserts the response into the database while also assigning position indices to items.
     */
    private fun insertResultIntoDb(body: ApiResponse?) {
        body?.let { it ->
            db.runInTransaction {
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
    private fun refresh(): LiveData<NetworkState> {
        val networkState = MutableLiveData<NetworkState>()
        networkState.value = NetworkState.LOADING
        coronaTrackerApi.scrape().enqueue(
                object : Callback<String> {
                    override fun onFailure(call: Call<String>, t: Throwable) {
                        // retrofit calls this on main thread so safe to call set value
                        networkState.value = NetworkState.error(t.message)
                    }

                    override fun onResponse(
                            call: Call<String>,
                            response: Response<String>) {
                        ioExecutor.execute {
                            db.runInTransaction {
                                updateResult(ApiResponse.fromString(response.body().toString()))
                            }
                            // since we are in bg thread now, post the result.
                            networkState.postValue(NetworkState.LOADED)
                        }
                    }
                }
        )
        return networkState
    }

    /**
     * Returns a Listing for the given data
     *
     */
    @MainThread
    override fun areasOfCoronaTracker(pageSize: Int): Listing<Area> {
        // create a boundary callback which will observe when the user reaches to the edges of
        // the list and update the database with extra data.
        val boundaryCallback = CoronaTrackerBoundaryCallback(
                webservice = coronaTrackerApi,
                handleResponse = this::insertResultIntoDb,
                insertFromFile = this::insertResultIntoDb,
                ioExecutor = ioExecutor,
                networkPageSize = networkPageSize)
        // we are using a mutable live data to trigger refresh requests which eventually calls
        // refresh method and gets a new live data. Each refresh request by the user becomes a newly
        // dispatched data in refreshTrigger
        val refreshTrigger = MutableLiveData<Unit>()
        val refreshState = refreshTrigger.switchMap {
            refresh()
        }

        // We use toLiveData Kotlin extension function here, you could also use LivePagedListBuilder
        val livePagedList = db.dao().areas().toLiveData(
                pageSize = pageSize,
                boundaryCallback = boundaryCallback)

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

