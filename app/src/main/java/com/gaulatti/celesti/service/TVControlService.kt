package com.gaulatti.celesti.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.gaulatti.celesti.device.DeviceManager
import com.gaulatti.celesti.model.Command
import com.gaulatti.celesti.network.SSEClient
import com.gaulatti.celesti.player.CommandHandler

/**
 * Foreground service that maintains the SSE connection and handles incoming commands.
 * Runs continuously in the background to listen for remote control commands.
 */
class TVControlService : Service() {
    
    private val TAG = "TVControlService"
    private val CHANNEL_ID = "TVControlChannel"
    private val NOTIFICATION_ID = 1
    
    private lateinit var sseClient: SSEClient
    private lateinit var commandHandler: CommandHandler
    private lateinit var deviceManager: DeviceManager
    private lateinit var deviceId: String
    private val mainHandler = Handler(Looper.getMainLooper())
    
    companion object {
        // Backend SSE endpoint URL
        private const val SSE_URL = "https://api.celesti.gaulatti.com/sse/events"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        // Create notification channel for foreground service
        createNotificationChannel()
        
        // Initialize device manager and get device ID
        deviceManager = DeviceManager(this)
        deviceId = deviceManager.getDeviceId()
        Log.d(TAG, "Device ID: $deviceId")
        
        // Log device info
        deviceManager.getDeviceInfo().forEach { (key, value) ->
            Log.d(TAG, "$key: $value")
        }
        
        // Initialize command handler
        commandHandler = CommandHandler(this)
        
        // Initialize SSE client with device ID and command callback
        val headers = mapOf(
            "X-Device-ID" to deviceId
        )
        sseClient = SSEClient(SSE_URL, deviceId, headers) { command ->
            // Post command handling to main thread
            mainHandler.post {
                handleCommand(command)
            }
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        
        // Start as foreground service
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        // Connect to SSE endpoint
        sseClient.connect()
        
        // Return START_STICKY to restart service if killed
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        // This service doesn't support binding
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        
        // Disconnect SSE client
        sseClient.disconnect()
        
        // Release command handler resources
        commandHandler.release()
    }
    
    /**
     * Handle incoming commands from the SSE stream.
     */
    private fun handleCommand(command: Command) {
        Log.d(TAG, "Handling command: ${command.type}")
        commandHandler.handle(command)
    }
    
    /**
     * Create notification channel for Android O and above.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TV Control Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the TV controller running in background"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Create the foreground service notification.
     */
    private fun createNotification(): Notification {
        // Device ID is already short (10 chars), show it all
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("TV Controller")
                .setContentText("Device: $deviceId - Listening...")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("TV Controller")
                .setContentText("Device: $deviceId - Listening...")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build()
        }
    }
}
