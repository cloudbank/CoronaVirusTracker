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
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.droidteahouse.GlideRequests
import com.droidteahouse.coronaTracker.R
import com.droidteahouse.coronaTracker.vo.Area
import java.text.DecimalFormat


/**
 * A RecyclerView ViewHolder that displays an area
 */
class AreaViewHolder(view: View, private val glide: GlideRequests
) : RecyclerView.ViewHolder(view) {

    private val name: TextView = view.findViewById(R.id.name)
    private val total: TextView = view.findViewById(R.id.total)
    private val recovered: TextView = view.findViewById(R.id.recovered)
    private val thumbnail: ImageView = view.findViewById(R.id.thumbnail)
    private val percentage: ImageView = view.findViewById(R.id.thumbnail)
    private val df by lazy { DecimalFormat("#.##") }
    private var area: Area? = null

    fun bind(area: Area?) {
        this.area = area
        name.text = area?.displayName


        glide.load(area?.imageUrl)
                .fitCenter()
                .placeholder(android.R.drawable.btn_star)
                .into(thumbnail)

        total.text = area?.totalConfirmed.toString()
        recovered.text = area?.totalRecovered.toString()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            recovered.tooltipText = df.format(area?.percentageRecovered).toString() + "%"
        }

    }

    companion object {
        fun create(parent: ViewGroup, glide: GlideRequests): AreaViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.area_item, parent, false)
            return AreaViewHolder(view, glide)
        }
    }

    fun updateCases(item: Area?) {
        area?.totalConfirmed = item?.totalConfirmed!!
        area?.totalRecovered = item?.totalRecovered
        area?.totalDeaths = item.totalDeaths
        total.text = "${item?.totalDeaths ?: 0}"

    }
}