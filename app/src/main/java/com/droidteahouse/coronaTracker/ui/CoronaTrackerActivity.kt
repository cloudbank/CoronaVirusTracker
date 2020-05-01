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
import android.graphics.PorterDuff
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.paging.PagedList
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.droidteahouse.GlideApp
import com.droidteahouse.GlideRequests
import com.droidteahouse.backdrop.BackDropIconClickListener
import com.droidteahouse.coronaTracker.R
import com.droidteahouse.coronaTracker.ServiceLocator
import com.droidteahouse.coronaTracker.repository.NetworkState
import com.droidteahouse.coronaTracker.vo.Area
import kotlinx.android.synthetic.main.activity_coronatracker.*
import kotlinx.android.synthetic.main.main_layout.*


/**
 * A list activity that shows areas affected by corona virus
 *
 */
class CoronaTrackerActivity : AppCompatActivity() {
    var network: MutableLiveData<Boolean> = MutableLiveData<Boolean>()

    companion object {

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
        setContentView(R.layout.main_layout)
        network.observe(this, Observer {
            no_network.visibility = if (it == true) View.GONE else View.VISIBLE
            no_network.invalidate()
        })
        initMenu()
        initSwipeToRefresh()
        initAdapter()
        checkNetwork()
    }

    private fun initMenu() {
        setSupportActionBar(app_bar)
        app_bar.setNavigationOnClickListener(BackDropIconClickListener(
                this,
                product_grid,
                ContextCompat.getDrawable(this, R.drawable.shr_branded_menu), // Menu open icon
                ContextCompat.getDrawable(this, R.drawable.shr_close_menu))) // Menu close icon
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            product_grid.background = getDrawable(R.drawable.shr_product_grid_background_shape)
        }

    }


    //@todo this will not work with a VPN connection like reverse tethering to detect if the relay server is cut
    private fun checkNetwork(): Boolean {
        var result = false
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            val activeNetworkInfo = cm.getActiveNetworkInfo()
            result = activeNetworkInfo != null && activeNetworkInfo.isConnected
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork
            val capabilities = cm
                    .getNetworkCapabilities(network)
            result = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
        network.value = result
        if (result == false) swipe_refresh.isRefreshing = result
        return result
    }


    private fun initAdapter() {
        val glide = GlideApp.with(this)
        loadWorldMap(glide)
        val adapter = AreaAdapter(glide) {
            if (checkNetwork()) {
                model.retry()
                loadWorldMap(glide)
            }
        }
        list.adapter = adapter
        val horizontalDecoration = DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL)
        val horizontalDivider = ContextCompat.getDrawable(this, R.drawable.list_divider)
        horizontalDecoration.setDrawable(horizontalDivider!!)
        list.addItemDecoration(horizontalDecoration)

        list.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        list.setHasFixedSize(true);
        list.setItemAnimator(DefaultItemAnimator())



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
        if (!getResources().getBoolean(R.bool.is_landscape)) {
            glide.load(getString(R.string.world_map_url)).into(mapview)
            mapview.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
        }
    }

    private fun initSwipeToRefresh() {
        swipe_refresh.setColorSchemeResources(
                R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark)
        model.refreshState.observe(this, Observer {
            swipe_refresh.isRefreshing = it == NetworkState.LOADING
            checkNetwork()
        })

        swipe_refresh.setOnRefreshListener {
            if (checkNetwork()) {
                model.refresh()
            }
        }
    }

}
