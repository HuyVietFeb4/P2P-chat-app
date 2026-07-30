package com.meshenger.backend.application.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    private const val CHANNEL_MESSAGES_ID = "meshenger_messages_channel"
    private const val CHANNEL_INVITES_ID = "meshenger_invites_channel"
    private const val MESSAGE_NOTIFICATION_ID = 1001
    private const val INVITE_NOTIFICATION_ID = 1002

    private fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Messages Channel
            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES_ID,
                "Meshenger Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new chat messages"
            }
            
            // Invites Channel
            val invitesChannel = NotificationChannel(
                CHANNEL_INVITES_ID,
                "Meshenger Connection Requests",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new pending connection requests"
            }
            
            notificationManager.createNotificationChannel(messagesChannel)
            notificationManager.createNotificationChannel(invitesChannel)
        }
    }

    private fun getLaunchIntent(context: Context): PendingIntent? {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (launchIntent != null) {
            launchIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            return PendingIntent.getActivity(
                context, 
                0, 
                launchIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        return null
    }

    fun showNewMessageNotification(context: Context, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        initChannels(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGES_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        getLaunchIntent(context)?.let {
            builder.setContentIntent(it)
        }

        val uniqueId = MESSAGE_NOTIFICATION_ID + (title.hashCode() % 10000)
        NotificationManagerCompat.from(context).notify(uniqueId, builder.build())
    }

    fun showPendingInviteNotification(context: Context, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        initChannels(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_INVITES_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        getLaunchIntent(context)?.let {
            builder.setContentIntent(it)
        }

        val uniqueId = INVITE_NOTIFICATION_ID + (title.hashCode() % 10000)
        NotificationManagerCompat.from(context).notify(uniqueId, builder.build())
    }
}
