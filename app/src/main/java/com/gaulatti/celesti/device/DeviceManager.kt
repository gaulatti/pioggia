package com.gaulatti.celesti.device

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlin.random.Random

/**
 * Manages device identification and registration.
 * Generates a unique device ID on first launch and persists it.
 */
class DeviceManager(context: Context) {
    
    private val TAG = "DeviceManager"
    private val PREFS_NAME = "celesti_device_prefs"
    private val KEY_DEVICE_ID = "device_id"
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Characters for ID generation (avoiding ambiguous chars: 0/O, 1/I/L)
    private val CHARSET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"
    
    /**
     * Get or generate the unique device identifier.
     * This ID is stable across app restarts and uniquely identifies this Fire TV device.
     * Format: Short 10-character alphanumeric code (e.g., "X4K7N9P2QR")
     */
    fun getDeviceId(): String {
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        
        if (deviceId == null) {
            // Generate short random ID (10 chars, easy to read and type)
            deviceId = generateShortId(10)
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
            Log.d(TAG, "Generated new device ID: $deviceId")
        } else {
            Log.d(TAG, "Retrieved existing device ID: $deviceId")
        }
        
        return deviceId
    }
    
    /**
     * Generate a short random alphanumeric ID.
     */
    private fun generateShortId(length: Int): String {
        return (1..length)
            .map { CHARSET[Random.nextInt(CHARSET.length)] }
            .joinToString("")
    }
    
    /**
     * Get device information for debugging/logging.
     */
    fun getDeviceInfo(): Map<String, String> {
        return mapOf(
            "deviceId" to getDeviceId(),
            "manufacturer" to android.os.Build.MANUFACTURER,
            "model" to android.os.Build.MODEL,
            "device" to android.os.Build.DEVICE
        )
    }
}
