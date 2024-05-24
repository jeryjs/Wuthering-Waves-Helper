package com.jery.wuwahelper.data

import android.content.Context
import androidx.core.content.ContextCompat
import com.jery.wuwahelper.R
import com.jery.wuwahelper.activity.MainActivity.Companion.getAppContext

data class EventItem(
    val event: String,
    val image: String,
    val duration: Pair<String, String>,
    val type: String,
    val status: String,
    val page: String
) {
    var isSeen: Boolean
        get() = try { getAppContext().getSharedPreferences("ItemsList", Context.MODE_PRIVATE).getBoolean("$event@$image", false) } catch (_:Exception) { false }
        set(value) { getAppContext().getSharedPreferences("ItemsList", Context.MODE_PRIVATE).edit().putBoolean("$event@$image", value).apply() }

    fun getSeenStatusColor(): Int {
        return if (isSeen)
            ContextCompat.getColor(getAppContext(), R.color.colorAccentVariant)
        else
            ContextCompat.getColor(getAppContext(), R.color.green)
    }

}
