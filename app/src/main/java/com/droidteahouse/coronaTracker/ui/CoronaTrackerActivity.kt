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


import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.droidteahouse.GlideApp
import com.droidteahouse.GlideRequests

import com.droidteahouse.coronaTracker.R
import com.droidteahouse.coronaTracker.ServiceLocator
import com.droidteahouse.coronaTracker.repository.NetworkState
import com.droidteahouse.coronaTracker.vo.Area

import kotlinx.android.synthetic.main.activity_coronatracker.*


/**
 * A list activity that shows areas affected by corona virus
 *
 */
class CoronaTrackerActivity : AppCompatActivity() {

    companion object {

        fun intentFor(context: Context): Intent {
            val intent = Intent(context, CoronaTrackerActivity::class.java)
            return intent
        }
    }

    private val model: CoronaTrackerViewModel by viewModels {
        object : AbstractSavedStateViewModelFactory(this, null) {
            override fun <T : ViewModel?> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle
            ): T {
                val repo = ServiceLocator.instance(this@CoronaTrackerActivity)
                        .getRepository()
                @Suppress("UNCHECKED_CAST")
                return CoronaTrackerViewModel(repo) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_coronatracker)

        initAdapter()
        initSwipeToRefresh()
    }


    private fun initAdapter() {
        val glide = GlideApp.with(this)
        loadWorldMap(glide)
        val adapter = AreaAdapter(glide) {
            model.retry()
            loadWorldMap(glide)
        }
        list.adapter = adapter
        list.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        model.areas.observe(this, Observer<PagedList<Area>> {
            adapter.submitList(it) {
                // Workaround for an issue where RecyclerView incorrectly uses the loading / spinner
                // item added to the end of the list as an anchor during initial load.
                val layoutManager = (list.layoutManager as LinearLayoutManager)
                val position = layoutManager.findFirstCompletelyVisibleItemPosition()
                if (position != RecyclerView.NO_POSITION) {
                    list.scrollToPosition(position)
                }
            }
        })
        model.networkState.observe(this, Observer {
            adapter.setNetworkState(it)
        })
        model.worldData.observe(this, Observer {
            totals.text = it
        })

    }

    private fun loadWorldMap(glide: GlideRequests) {
        glide.load(getString(R.string.world_map_url)).into(mapview)
        mapview.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimaryDark), PorterDuff.Mode.MULTIPLY);
    }

    private fun initSwipeToRefresh() {

        model.refreshState.observe(this, Observer {
            swipe_refresh.isRefreshing = it == NetworkState.LOADING
        })
        swipe_refresh.setOnRefreshListener {
            model.refresh()
        }
    }

}
