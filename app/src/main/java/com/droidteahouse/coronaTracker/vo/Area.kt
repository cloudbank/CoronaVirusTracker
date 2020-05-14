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

package com.droidteahouse.coronaTracker.vo

import androidx.annotation.Nullable
import androidx.room.*


@Entity(tableName = "areas",
        indices = [Index(value = ["id"], unique = false)])
data class Area(
        @ColumnInfo(defaultValue = "0") var totalConfirmed: String = "",
        var totalConfirmedInt: Int = 0,
        @Nullable
        @ColumnInfo(defaultValue = "0") var totalDeaths: String? = "",
        @Nullable
        var totalRecovered: String? = "",
        @PrimaryKey
        var id: String = "",
        var displayName: String = "",
        var pageUrl: String = "",
        var imageUrl: String = "") {
    constructor(id: String, totalRecovered: Int, totalConfirmed: Int, totalDeaths: Int) : this()

    @Ignore
    var percentageRecovered: Double =
            if (totalRecovered.isNullOrBlank() || totalRecovered is String || totalRecovered == "" || totalRecovered?.replace(",", "")!!.toInt() == 0) 0.0 else totalRecovered?.replace(",", "")!!.toDouble() / (totalConfirmed.replace(",", "").toDouble() - (totalDeaths?.replace(",", "")!!.toDouble())) * 100.00

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?) = (other is Area)
            && id == other.id
            && totalConfirmed == other.totalConfirmed
            && totalRecovered == other.totalRecovered
            && totalDeaths == other.totalDeaths


}