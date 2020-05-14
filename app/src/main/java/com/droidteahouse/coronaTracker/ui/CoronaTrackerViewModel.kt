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
import com.droidteahouse.coronaTracker.repository.Listing
import com.droidteahouse.coronaTracker.vo.ApiResponse
import com.droidteahouse.coronaTracker.vo.Area
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class CoronaTrackerViewModel(
        private val repository: CoronaTrackerRepository

) : ViewModel() {


    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelScope.coroutineContext)


    val worldLiveData: LiveData<ApiResponse> =
            liveData<ApiResponse>(context = viewModelScope.coroutineContext + Dispatchers.IO) { repository.worldData() }

    var worldData: LiveData<String> = Transformations.map(worldLiveData) { data ->
        if (data?.totalConfirmed == null) " " else "${data?.totalConfirmed} total  ${data?.totalRecovered} recovered"
    }


    private val repoResult = liveData<Listing<Area>>(context = viewModelScope.coroutineContext + Dispatchers.IO) {
        emit(repository.areasOfCoronaTracker(30, uiScope))
    }

    //livedata vs databinding
    //a livedata that observes the value of the button in the menu
    //mapping that to a repo call
    //this vm

    val resultList = repoResult.switchMap { it.pagedList }
    val networkState = repoResult.switchMap { it.networkState }
    val refreshState = repoResult.switchMap { it.refreshState }


    fun refresh() {
        viewModelScope.launch {
            repoResult.value?.refresh?.invoke()
        }
    }

    fun retry() {
        repoResult.value?.retry?.invoke()
    }


}
