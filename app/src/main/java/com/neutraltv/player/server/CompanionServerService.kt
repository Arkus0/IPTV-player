package com.neutraltv.player.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.neutraltv.core.discovery.NsdDiscoveryManager
import com.neutraltv.core.protocol.ApiRoutes
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CompanionServerService : Service() {

    companion object {
        private const val TAG = "CompanionService"
        private const val CHANNEL_ID = "companion_server"
        private const val NOTIFICATION_ID = 1001
    }

    @Inject lateinit var companionServer: CompanionServer
    @Inject lateinit var nsdDiscoveryManager: NsdDiscoveryManager

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        companionServer.start()
        nsdDiscoveryManager.registerService(companionServer.port)
        Log.d(TAG, "Companion server service started")
    }

    override fun onDestroy() {
        nsdDiscoveryManager.unregisterService()
        companionServer.stop()
        Log.d(TAG, "Companion server service stopped")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Servidor Companion",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene el servidor activo para la app móvil"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("JuanPlayer Companion")
            .setContentText("Servidor activo para control remoto")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build()
    }
}
