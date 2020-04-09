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

package com.droidteahouse.coronaTracker.ui

import androidx.lifecycle.*
import com.droidteahouse.coronaTracker.repository.CoronaTrackerRepository
import com.droidteahouse.coronaTracker.vo.ApiResponse


class CoronaTrackerViewModel(
        private val repository: CoronaTrackerRepository

) : ViewModel() {
    val worldLiveData: LiveData<ApiResponse> =
            repository.worldData()

    var worldData: LiveData<String> = Transformations.map(worldLiveData) { data ->
        if (data?.totalConfirmed == null) " " else "${data?.totalConfirmed} total  ${data?.totalRecovered} recovered"
    }
    private val repoResult = repository.areasOfCoronaTracker(30)


    val areas = repoResult.pagedList
    val networkState = repoResult.networkState
    val refreshState = repoResult.refreshState

    fun refresh() {
        repoResult.refresh?.invoke()
    }


    fun retry() {
        val listing = repoResult
        listing?.retry?.invoke()
    }
}
