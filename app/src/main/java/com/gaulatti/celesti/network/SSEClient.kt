package com.gaulatti.celesti.network

import android.util.Log
import com.gaulatti.celesti.model.Command
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.time.Duration
import kotlin.math.min

class SSEClient(
    private val url: String,
    private val deviceId: String,
    private val headers: Map<String, String> = emptyMap(),
    private val onCommand: (Command) -> Unit
) {
    private val TAG = "SSEClient"
    private val gson = Gson()
    private var job: Job? = null
    private var backoffDelay = 1000L
    private val maxBackoffDelay = 30000L

    private val client = OkHttpClient.Builder()
        .readTimeout(Duration.ZERO)
        .build()

    fun connect() {
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    // Build URL with device ID parameter
                    val urlWithDevice = "$url?deviceId=$deviceId"
                    Log.d(TAG, "Connecting to $urlWithDevice")
                    
                    val requestBuilder = Request.Builder()
                        .url(urlWithDevice)
                        .header("Accept", "text/event-stream")
                        .header("X-Device-ID", deviceId)
                    
                    // Add any additional headers
                    headers.forEach { (key, value) ->
                        requestBuilder.header(key, value)
                    }
                    
                    val request = requestBuilder.build()

                    // execute() is blocking - this is what we want inside a coroutine
                    val response = client.newCall(request).execute()

                    if (!response.isSuccessful) {
                        Log.e(TAG, "HTTP ${response.code}")
                        response.close()
                        delay(backoffDelay)
                        backoffDelay = min(backoffDelay * 2, maxBackoffDelay)
                        continue
                    }

                    // Reset backoff on successful connection
                    backoffDelay = 1000L
                    Log.d(TAG, "Connected")

                    // This blocks until the connection drops
                    response.body?.use { body ->
                        processEventStream(body.charStream().buffered())
                    }

                    Log.d(TAG, "Connection closed")

                } catch (e: CancellationException) {
                    Log.d(TAG, "Cancelled")
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error: ${e.message}")
                }

                // Reconnect with backoff
                Log.d(TAG, "Reconnecting in ${backoffDelay}ms")
                delay(backoffDelay)
                backoffDelay = min(backoffDelay * 2, maxBackoffDelay)
            }
        }
    }

    private fun processEventStream(reader: BufferedReader) {
        val buffer = StringBuilder()
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            val currentLine = line ?: continue

            when {
                currentLine.startsWith("data:") -> {
                    if (buffer.isNotEmpty()) buffer.append("\n")
                    buffer.append(currentLine.substring(5).trim())
                }
                currentLine.isEmpty() && buffer.isNotEmpty() -> {
                    try {
                        val command = gson.fromJson(buffer.toString(), Command::class.java)
                        Log.d(TAG, "Command: $command")
                        onCommand(command)
                    } catch (e: Exception) {
                        Log.e(TAG, "Parse error: ${e.message}")
                    }
                    buffer.clear()
                }
                // comments (lines starting with :) and other fields - ignore
            }
        }
    }

    fun disconnect() {
        job?.cancel()
        job = null
    }
}
