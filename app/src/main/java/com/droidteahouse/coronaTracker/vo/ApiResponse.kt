package com.droidteahouse.coronaTracker.vo

import android.content.Context
import android.net.Uri
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.io.IOException
import java.io.InputStream


@Entity(tableName = "coronatracker",
        indices = [Index(value = ["id"], unique = false)])
data class ApiResponse(
        @PrimaryKey
        val id: String,
        val totalConfirmed: String,
        val totalDeaths: String,
        val totalRecovered: String,
        var areas: List<Area> = mutableListOf(),
        val lastUpdated: String = ""

) {

    override fun toString(): String {
        return totalConfirmed.toString() + " | " + totalRecovered
    }

    companion object {
        private var worldData: List<String> = ArrayList<String>()
        private val countries: ArrayList<String> = ArrayList()
        const val startString = "<table class=\"wikitable plainrowheaders sortable\""
        const val endString = "</table>"
        const val imageStart = "src"
        const val imageEnd = "decoding"
        const val world = "world"

        fun fromFile(ctx: Context): ApiResponse {
            val gson = Gson()
            val realData = loadJSONFromAsset(ctx)
            return gson.fromJson(realData, object : TypeToken<ApiResponse>() {}.type) as ApiResponse
        }

        fun fromString(s: String): ApiResponse {

            val start = s.indexOf(startString)
            val data = s.subSequence(start, s.indexOf(endString, start)).toString()
            val doc: Document = Jsoup.parse(data)
            parseTable(doc)

            val areas = mutableListOf<Area>()


            val apiResponse = ApiResponse(id = world, totalDeaths = worldData[2], totalConfirmed = worldData[1], totalRecovered = worldData[3])

            for (i in 0 until countries.size - 1) {

                val areaData = countries[i].split(" ")
                val a = Area()
                a.id = areaData[4].toLowerCase().replace("_", "")  //map
                a.displayName = areaData[4].replace("_", " ")
                a.totalRecovered = areaData[2]
                a.totalConfirmed = areaData[0]
                a.totalConfirmedInt = areaData[0]?.replace(",", "").toInt()
                a.totalDeaths = areaData[1]
                a.imageUrl = areaData[5].replace("src=\"", "https:").replace("\"", "")

                areas.add(a)
            }
            apiResponse.areas = areas
            return apiResponse
        }


        val regex = """\[..\]|\[.\]""".toRegex()
        private fun parseTable(doc: Document) {
            val table: Element = doc.select("table")[0] //select the first table.
            worldData = table.select("tr")[1].select("th").text().replace(regex, "").split(" ")

            val tbody: Element = table.select("tbody")[0]
            val rows: Elements = tbody.select("tr")

            for (i in 2 until rows.size - 2) {
                val row: Element = rows[i]
                var imageUrl = row.select("th")[0].child(0).toString()
                val start = imageUrl.indexOf(imageStart)
                imageUrl = imageUrl.subSequence(start, imageUrl.indexOf(imageEnd, start)).toString()
                val country = row.select("th")[1].text()
                val cols: Elements = row.select("td")
                val s = (cols.text() + " " + country.replace(" ", "_") + " " + imageUrl).replace(regex, "")
                countries.add(s)
            }
        }

        fun loadJSONFromAsset(ctx: Context): String? {
            var json: String? = null
            json = try {
                val iss: InputStream = ctx.getAssets().open("database/init.json")
                val size: Int = iss.available()
                val buffer = ByteArray(size)
                iss.read(buffer)
                iss.close()
                String(buffer)
            } catch (ex: IOException) {
                ex.printStackTrace()
                return null
            }
            return json
        }
    }
}
