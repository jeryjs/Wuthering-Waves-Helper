package com.jery.starrailhelper.tasks

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jery.starrailhelper.R
import com.jery.starrailhelper.activity.MainActivity
import com.jery.starrailhelper.data.CodeItem
import com.jery.starrailhelper.utils.Utils
import com.jery.starrailhelper.utils.Utils.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CodeCheckReceiver : BroadcastReceiver() {

    companion object {
        private const val NOT_CHANNEL_NAME = "New Codes Notification"
        private const val NOT_CHANNEL_ID = "new_code_notification_channel"
        private const val NOT_ID = 123
        private const val TAG = "CodeCheckReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Reschedule the code check worker on device reboot
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CodeCheckScheduler.scheduleCodeCheck(context)
            log(TAG, "Boot completed")
        }
        CoroutineScope(Dispatchers.Default).launch {
            log(TAG, "Codes Check signal received")
            try {
                val newCodes = Utils.fetchCodes().first.filter { it.isNewCode }
                if (newCodes.isNotEmpty())
                    showNotification(context, newCodes)
            } catch (e: Exception) {
                log(TAG, "Failed to fetch new codes: $e")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(context: Context, newCodes: List<CodeItem>) {
        log(TAG, "Showing notification\t[Found ${newCodes.size} new codes]")

        val notificationManager = NotificationManagerCompat.from(context)

        val channel = NotificationChannel(NOT_CHANNEL_ID, NOT_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        val notificationBuilder = NotificationCompat.Builder(context, NOT_CHANNEL_ID)
            .setContentTitle("New Codes Available")
            .setContentText("You have ${newCodes.size} new codes.")
            .setSmallIcon(R.drawable.ic_item_stellar_jade)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setAutoCancel(true)
            .setContentIntent( PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ))

        notificationManager.notify(NOT_ID, notificationBuilder.build())
    }
}
