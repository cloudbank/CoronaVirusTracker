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


import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import com.droidteahouse.backdrop.BackDropIconClickListener
import com.droidteahouse.coronaTracker.R
import com.droidteahouse.coronaTracker.ServiceLocator
import kotlinx.android.synthetic.main.main_layout.*


/**
 * A list activity that shows areas affected by corona virus
 *
 */
class CoronaTrackerActivity : AppCompatActivity() {



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

        initMenu()
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
        //product_grid.onTouchEvent()
    }

    fun updateData(v: View) {
        val nc = myNavHostFragment.findNavController()
        when (v.id) {
            R.id.dailyreportbtn -> nc.navigate(R.id.daily_report)
            R.id.news -> nc.navigate(R.id.newsFragment)
        }
    }


}
