package com.jery.wuwahelper.data

import android.content.Context
import androidx.core.content.ContextCompat
import com.jery.wuwahelper.R
import com.jery.wuwahelper.activity.MainActivity.Companion.getAppContext

data class CodeItem(
    val code: String,
    val server: String,
    val rewards: List<RewardItem>,
    val duration: Pair<String, String>,
    val isExpired: Boolean = false
) {
    var rewardsString: String? = null
    var isNewCode = false
    var isRedeemed: Boolean
        get() = getRedeemedStatus()
        set(data) = setRedeemedStatus(data)

    fun getRedeemedStatusColor(): Int {
        return if (isRedeemed)
            ContextCompat.getColor(getAppContext(), R.color.colorAccentVariant)
        else if (isExpired)
            ContextCompat.getColor(getAppContext(), R.color.colorAccent)
        else
            ContextCompat.getColor(getAppContext(), R.color.green)
    }

    init {
        rewardsString = rewards.joinToString(", ") { "${it.name} x${it.quantity}" }
        isRedeemed = getRedeemedStatus()
//        Log.d("CodeItem", "Processing CODE: $code")
    }

    private fun getRedeemedStatus(): Boolean {
        return try {
            val codesPrefs = getAppContext().getSharedPreferences("ItemsList", Context.MODE_PRIVATE)
            if (!codesPrefs.contains(code)) {
                isNewCode = true
                setRedeemedStatus(false)
            }
            return codesPrefs.getBoolean(code, false)
        } catch (_:Exception) { false }
    }
    private fun setRedeemedStatus(status: Boolean) {
        try {
            val codesPrefs = getAppContext().getSharedPreferences("ItemsList", Context.MODE_PRIVATE)
            val editor = codesPrefs.edit()
            editor.putBoolean(code, status)
            editor.apply()
        } catch (_:Exception) { }
    }
}
