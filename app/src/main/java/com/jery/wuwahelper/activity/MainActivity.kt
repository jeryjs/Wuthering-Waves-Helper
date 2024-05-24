package com.jery.wuwahelper.activity

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.jery.wuwahelper.R
import com.jery.wuwahelper.adapter.CodeAdapter
import com.jery.wuwahelper.adapter.EventAdapter
import com.jery.wuwahelper.data.CodeItem
import com.jery.wuwahelper.data.EventItem
import com.jery.wuwahelper.databinding.ActivityMainBinding
import com.jery.wuwahelper.tasks.CodeCheckScheduler
import com.jery.wuwahelper.utils.Utils
import kotlinx.coroutines.launch
import java.io.File


class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val PREFS_NAME = "myPrefs"
        private const val SWITCH_STATE = "switchState"

        lateinit var webEventsAdapter: EventAdapter
        lateinit var activeCodesAdapter: CodeAdapter
        lateinit var expiredCodesAdapter: CodeAdapter
        lateinit var binding: ActivityMainBinding

        var mContext: Context? = null
        fun getAppContext(): Context {
            return mContext!!
        }
    }
    private val webEvents = mutableListOf<EventItem>()
    private val activeCodes = mutableListOf<CodeItem>()
    private val expiredCodes = mutableListOf<CodeItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = applicationContext
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {

            setupRecyclerViews()
            refreshAll()

            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val editor = prefs.edit()

            binding.notiStatus.isChecked = prefs.getBoolean(SWITCH_STATE, false)
            if (binding.notiStatus.isChecked) CodeCheckScheduler.scheduleCodeCheck(this)

            binding.notiStatus.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    val permission = "android.permission.POST_NOTIFICATIONS"
                    try {
                        ActivityCompat.requestPermissions(this, arrayOf(permission), 1)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    CodeCheckScheduler.scheduleCodeCheck(this)
                    Toast.makeText(
                        this,
                        "Checking for new codes periodically!!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    CodeCheckScheduler.cancelCodeCheck(this)
                    Toast.makeText(this, "Stopped checking for new codes!!", Toast.LENGTH_SHORT).show()
                }
                editor.putBoolean(SWITCH_STATE, isChecked)
                editor.apply()
            }

            @Suppress("SdCardPath")     // Hardcoding '/data/data' to workaround the need to use context
            binding.appBarTitle.setOnLongClickListener {
                val scrollView = ScrollView(this)
                val horizontalScrollView = HorizontalScrollView(this)
                val textView = TextView(this).apply {
                    text = File("/data/data/$packageName/files/logs.txt").readText()
                    setPadding(16, 16, 16, 16)
                    isHorizontalScrollBarEnabled = true
                    setTextIsSelectable(true)
                }
                horizontalScrollView.addView(textView)
                scrollView.addView(horizontalScrollView)

                val alertDialog = AlertDialog.Builder(this)
                    .setTitle("Logs")
                    .setView(scrollView)
                    .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
                    .setNeutralButton("Copy") { _, _ -> Utils.copyToClipboard(textView.text.toString(), binding.root) }
                    .create()
                alertDialog.show()
                true
            }

            // Easter Egg: change app theme from night to day or day to night
            binding.appBarIcon.setOnLongClickListener {
                if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES)
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                else
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                true
            }

            // Easter Egg: show hidden settings menu
            binding.notiStatus.setOnLongClickListener { showSettingsMenu(null); true}

            binding.swipeRefresh.setOnRefreshListener {
                refreshAll()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Utils.showStackTrace(this, e)
        }
    }

    private fun setupRecyclerViews() {
        webEventsAdapter = EventAdapter(webEvents)
        activeCodesAdapter = CodeAdapter(activeCodes)
        expiredCodesAdapter = CodeAdapter(expiredCodes)
        with (binding) {
            rvCurEvents.apply {
                layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
                adapter = webEventsAdapter
            }
            rvActiveCodes.apply {
                adapter = activeCodesAdapter
                layoutManager = LinearLayoutManager(this@MainActivity)
            }
            rvExpiredCodes.apply {
                adapter = expiredCodesAdapter
                layoutManager = LinearLayoutManager(this@MainActivity)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshAll() {
        lifecycleScope.launch {
            binding.swipeRefresh.isRefreshing = true
            val events = try { Utils.fetchEvents() } catch (e: Exception) {
                Utils.log(TAG, "Failed to fetch events: $e")
                Snackbar.make(binding.root, "Failed to fetch events: $e", Snackbar.LENGTH_SHORT).show()
                e.printStackTrace()
                binding.swipeRefresh.isRefreshing = false
                return@launch
            }
            updateEvents(events)
            webEventsAdapter.notifyDataSetChanged()
            binding.swipeRefresh.isRefreshing = false
        }
        lifecycleScope.launch {
            binding.swipeRefresh.isRefreshing = true
            val (ac, ec) = try { Utils.fetchCodes() } catch (e: Exception) {
                Utils.log(TAG, "Failed to fetch codes: $e")
                Snackbar.make(binding.root, "Failed to fetch codes: $e", Snackbar.LENGTH_SHORT).show()
                e.printStackTrace()
                binding.swipeRefresh.isRefreshing = false
                return@launch
            }
//            val (ac, ec) = Utils.demoCodes()
            updateCodes(ac, ec)
            notifyNewCodes(activeCodes)
            activeCodesAdapter.notifyDataSetChanged()
            expiredCodesAdapter.notifyDataSetChanged()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun updateEvents(events: List<EventItem>) {
        webEvents.apply {clear(); addAll(events) }
    }
    private fun updateCodes(nac: List<CodeItem>, nec: List<CodeItem>) {
        activeCodes.apply { clear(); addAll(nac) }
        expiredCodes.apply { clear(); addAll(nec) }
    }

    private fun notifyNewCodes(codes: List<CodeItem>) {
        val newCodes = codes.filter { it.isNewCode }
        val codesString = newCodes.joinToString(", ", transform = { it.code })
            .let { if (it.isNotEmpty()) "New codes: $it" else "No new codes" }
//        if (newCodes.isNotEmpty())
//        CodeCheckReceiver().showNotification(this, newCodes)
//        CodeCheckScheduler.scheduleCodeCheck(this)
        Snackbar.make(binding.root, codesString, Snackbar.LENGTH_LONG).show()
    }

    fun showSettingsMenu(item: MenuItem?) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_layout, binding.root)
        bottomSheetDialog.setContentView(dialogView)

        bottomSheetDialog.show()

//        val settingsFragment = SettingsFragment()
//        supportFragmentManager.beginTransaction()
//            .replace(frameLayout.id, settingsFragment)
//            .commit()
    }
}