package com.jery.wuwahelper.tasks

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.jery.wuwahelper.utils.Utils.log
import java.util.concurrent.TimeUnit

class CodeCheckScheduler {

    companion object {
        private const val TAG = "CodeCheckScheduler"

        fun scheduleCodeCheck(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            // Set the interval to 12 hours (12 * 60 * 60 * 1000 milliseconds)
            val intervalMillis = TimeUnit.HOURS.toMillis(24)
            // Set the initial trigger time to the current time
            val triggerTime = System.currentTimeMillis()
            // Create an explicit intent for the CodeCheckReceiver
            val intent = Intent(context, CodeCheckReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // Schedule the repeating alarm with the desired interval
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                intervalMillis,
                pendingIntent
            )
            log(TAG, "Scheduler scheduled to check for codes every ${TimeUnit.MILLISECONDS.toHours(intervalMillis)} hours")
        }

        fun cancelCodeCheck(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            // Create an explicit intent for the CodeCheckReceiver
            val intent = Intent(context, CodeCheckReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // Cancel the repeating alarm
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Codes Check schedule cancelled")
        }
    }
}
