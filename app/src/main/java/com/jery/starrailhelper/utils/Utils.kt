package com.jery.starrailhelper.utils

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.icu.text.SimpleDateFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.BaseTransientBottomBar.ANIMATION_MODE_SLIDE
import com.google.android.material.snackbar.Snackbar
import com.jery.starrailhelper.activity.MainActivity.Companion.getAppContext
import com.jery.starrailhelper.data.CodeItem
import com.jery.starrailhelper.data.EventItem
import com.jery.starrailhelper.data.RewardItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit


object Utils {
    fun copyToClipboard(text: String, view: View?) {
        try {
            val clipboard =
                getAppContext().getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("label", text)
            clipboard.setPrimaryClip(clip!!)
            if (view != null) {
                Snackbar.make(view, "Copied to clipboard $text", LENGTH_SHORT)
                    .setAnimationMode(ANIMATION_MODE_SLIDE).show()
            }
        } catch (e: Exception) {
            showStackTrace(getAppContext(), e)
        }
    }

//    suspend fun fetchEvents(): List<EventItem> = withContext(Dispatchers.IO) {
//        val url = URL("https://honkai-star-rail.fandom.com/wiki/Events")
//        val urlConnection: HttpURLConnection = url.openConnection() as HttpURLConnection
//        urlConnection.requestMethod = "GET"
//        val inputStream: InputStream = urlConnection.inputStream
//        val response = inputStream.bufferedReader().use(BufferedReader::readText)
//        val doc = Jsoup.parse(response)
//
//        val curEvents = doc.select(".wikitable")[0]!!.select("tbody > tr:not(tr:first-child)")
//        val upcEvents = doc.select(".wikitable")[1]!!.select("tbody > tr:not(tr:first-child)")
//        val perEvents = doc.select(".wikitable")[2]!!.select("tbody > tr:not(tr:first-child)")
//
//        val allEvents = curEvents.map {
//            val event = it.children()[0].text()
//            val image = it.select("img").attr("data-src").ifEmpty { it.select("img").attr("src") }.replace("scale-to-width-down/250", "scale-to-width-down/500")
//            val duration = parseDuration( it.children()[1].text() )
//            val type = it.children()[2].text()
//            EventItem(event, image, duration, type)
//        }
//        return@withContext allEvents.filter{it.type=="Web"}
//    }
    suspend fun fetchEvents(): List<EventItem> = withContext(Dispatchers.IO) {
        val response = fetch("https://raw.githubusercontent.com/jeryjs/data-scraper-for-star-rail-helper/main/star-rail-data.json")
        val eventsJson = JSONObject(response).getJSONObject("Events")
        val allEvents = mutableListOf<EventItem>()

        for (eventsType in eventsJson.keys()) {
            val eventsArray = eventsJson.getJSONArray(eventsType)
            for (i in 0 until eventsArray.length()) {
                val eventObj = eventsArray.getJSONObject(i)
                val event = eventObj.getString("event")
                val image = eventObj.getString("image")
                val duration = eventObj.getJSONArray("duration").let { it.optString(0, null) to it.optString(1, null) }
                val type = eventObj.getJSONArray("type")[0].toString()
                val status = eventObj.getString("status")
                val page = eventObj.getString("page")
                allEvents.add(EventItem(event, image, duration, type, status, page))
            }
        }

        return@withContext allEvents.filter { "Web" in it.type }
    }

    suspend fun fetchCodes(): Pair<List<CodeItem>, List<CodeItem>> = withContext(Dispatchers.IO) {
        val response = fetch("https://raw.githubusercontent.com/jeryjs/data-scraper-for-star-rail-helper/main/star-rail-data.json")
        val codesJson = JSONObject(response).getJSONObject("Codes")
        val allCodes = mutableListOf<CodeItem>()

        for (codesType in codesJson.keys()) {
            val codesArray = codesJson.getJSONArray(codesType)
            for (i in 0 until codesArray.length()) {
                val codeObj = codesArray.getJSONObject(i)
                val code = codeObj.getString("code")
                val server = codeObj.getString("server")
                val rewards = parseRewards(codeObj.getJSONArray("rewards"))
                val duration = codeObj.getJSONArray("duration").let { it.optString(0, null) to it.optString(1, null) }
                val isExpired = codeObj.getBoolean("isExpired")
                allCodes.add(CodeItem(code, server, rewards, duration, isExpired))
            }
        }

        return@withContext allCodes.partition { !it.isExpired }
    }
    private fun parseRewards(json: JSONArray): List<RewardItem> {
        val rewardsList = mutableListOf<RewardItem>()
        for (i in 0 until json.length()) {
            val item = json.getJSONObject(i)
            val name = item.getString("name")
            val amount = item.getInt("amount")
            val imageURL = item.getString("imageURL")
            rewardsList.add(RewardItem(name, amount, imageURL))
        }
        return rewardsList
    }

    @Suppress("MemberVisibilityCanBePrivate", "unused")
    fun demoCodes(): Pair<List<CodeItem>, List<CodeItem>> {
        return Pair(
            listOf(
                CodeItem("TESTCODE1024", "All", listOf(RewardItem("Condensed Aether",3), RewardItem("Credit",10000)), "01-01-2023" to "Unknown"),
                CodeItem("TESTCODE1024", "All", listOf(RewardItem("Condensed Aether",3), RewardItem("Credit",10000)), "01-01-2023" to "Unknown"),
                CodeItem("TESTCODE1024", "All", listOf(RewardItem("Condensed Aether",3), RewardItem("Credit",10000)), "01-01-2023" to "Unknown"),
            ),
            listOf(
                CodeItem("TESTCODE1024", "All", listOf(RewardItem("Condensed Aether",3), RewardItem("Credit",10000)), "01-01-2023" to "01-01-2023"),
                CodeItem("TESTCODE1024", "All", listOf(RewardItem("Condensed Aether",3), RewardItem("Credit",10000)), "01-01-2023" to "01-01-2023"),
                CodeItem("TESTCODE1024", "All", listOf(RewardItem("Condensed Aether",3), RewardItem("Credit",10000)), "01-01-2023" to "01-01-2023"),
                CodeItem("TESTCODE1024", "All", listOf(RewardItem("Condensed Aether",3), RewardItem("Credit",10000)), "01-01-2023" to "01-01-2023"),
                CodeItem("TESTCODE1024", "All", listOf(RewardItem("Condensed Aether",3), RewardItem("Credit",10000)), "01-01-2023" to "01-01-2023"),
                CodeItem("TESTCODE1024", "All", listOf(RewardItem("Condensed Aether",3), RewardItem("Credit",10000)), "01-01-2023" to "01-01-2023"),
                CodeItem("TESTCODE1024", "All", listOf(RewardItem("Condensed Aether",3), RewardItem("Credit",10000)), "01-01-2023" to "01-01-2023"),
                CodeItem("TESTCODE1024", "All", listOf(RewardItem("Condensed Aether",3), RewardItem("Credit",10000)), "01-01-2023" to "01-01-2023"),
                CodeItem("TESTCODE1024", "All", listOf(RewardItem("Condensed Aether",3), RewardItem("Credit",10000)), "01-01-2023" to "01-01-2023"),
                CodeItem("TESTCODE1024", "All", listOf(RewardItem("Condensed Aether",3), RewardItem("Credit",10000)), "01-01-2023" to "01-01-2023"),
            )
        )
    }

    @SuppressLint("SdCardPath")
    @Suppress("SameParameterValue")
    private fun fetch(url: String): String {
        val cacheDirectory = File("/data/data/com.jery.starrailhelper/cache", "okhttp-cache")
        val cacheSize = 5 * 1024 * 1024 // 5 MB
        val cache = Cache(cacheDirectory, cacheSize.toLong())

        val maxAge = 120 // Maximum age of cached responses in seconds

        val cacheControl = CacheControl.Builder()
            .maxAge(maxAge, TimeUnit.SECONDS)
            .build()

        val client = OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .cacheControl(cacheControl)
                    .build()
                chain.proceed(request)
            }
            .build()

        val request = Request.Builder().url(url).build()

        val response = client.newCall(request).execute()
        return response.body.string()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun showStackTrace(context: Context, e: Exception) {
        e.printStackTrace()
        val stackTrace = e.stackTraceToString()
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Stack Trace")
            builder.setMessage(stackTrace)
            builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            builder.setNeutralButton("Copy") { _, _ -> copyToClipboard(stackTrace, null) }
            val dialog = builder.create()
            dialog.show()
        }
    }

    @Suppress("SdCardPath")     // Hardcoding '/data/data' to workaround the need to use context
    fun log(tag: String, message: String) {
        Log.d(tag, message)
        val formatter = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.DEFAULT, SimpleDateFormat.DEFAULT, Locale.getDefault())
        val formattedDate: String = formatter.format(System.currentTimeMillis())
        val logString = "[$formattedDate]\t$tag:\t$message\n"
        val logFilePath = "/data/data/com.jery.starrailhelper/files/logs.txt"
        try {
            val outputStream = FileOutputStream(logFilePath, true) // Use append mode
            outputStream.write(logString.toByteArray())
            outputStream.close()
        } catch (e: Exception) {
            Log.e(tag, "Error writing to log file: ${e.stackTraceToString()}")
        }
    }
}