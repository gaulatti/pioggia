package com.gaulatti.celesti.network

import android.util.Log
import com.gaulatti.celesti.model.Command
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.EventListener
import okhttp3.Call
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

    private val eventListener = object : EventListener() {
        override fun callStart(call: Call) {
            Log.d(TAG, "EventListener: Call started")
        }
        
        override fun dnsStart(call: Call, domainName: String) {
            Log.d(TAG, "EventListener: DNS lookup started for $domainName")
        }
        
        override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<java.net.InetAddress>) {
            Log.d(TAG, "EventListener: DNS lookup completed: ${inetAddressList.joinToString()}")
        }
        
        override fun connectStart(call: Call, inetSocketAddress: java.net.InetSocketAddress, proxy: java.net.Proxy) {
            Log.d(TAG, "EventListener: Connecting to $inetSocketAddress")
        }
        
        override fun connectEnd(call: Call, inetSocketAddress: java.net.InetSocketAddress, proxy: java.net.Proxy, protocol: okhttp3.Protocol?) {
            Log.d(TAG, "EventListener: Connected via $protocol")
        }
        
        override fun connectionAcquired(call: Call, connection: okhttp3.Connection) {
            Log.d(TAG, "EventListener: Connection acquired")
        }
        
        override fun requestHeadersStart(call: Call) {
            Log.d(TAG, "EventListener: Sending request headers")
        }
        
        override fun requestHeadersEnd(call: Call, request: okhttp3.Request) {
            Log.d(TAG, "EventListener: Request headers sent")
        }
        
        override fun responseHeadersStart(call: Call) {
            Log.d(TAG, "EventListener: Receiving response headers")
        }
        
        override fun responseHeadersEnd(call: Call, response: okhttp3.Response) {
            Log.d(TAG, "EventListener: Response headers received: ${response.code}")
        }
        
        override fun callFailed(call: Call, ioe: java.io.IOException) {
            Log.e(TAG, "EventListener: Call failed", ioe)
        }
    }

    private val client = OkHttpClient.Builder()
        .readTimeout(Duration.ofSeconds(60))  // Timeout for initial response, then switches to streaming
        .connectTimeout(Duration.ofSeconds(30))
        .callTimeout(Duration.ZERO)
        .eventListener(eventListener)
        .build()

    fun connect() {
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    // Build URL with device code parameter
                    val urlWithDevice = "$url?device_code=$deviceId"
                    Log.d(TAG, "Connecting to $urlWithDevice")
                    
                    val requestBuilder = Request.Builder()
                        .url(urlWithDevice)
                        .header("Accept", "text/event-stream")
                        .header("Cache-Control", "no-cache")
                        .header("Connection", "keep-alive")
                        .header("X-Device-ID", deviceId)
                    
                    // Add any additional headers
                    headers.forEach { (key, value) ->
                        requestBuilder.header(key, value)
                    }
                    
                    val request = requestBuilder.build()
                    
                    Log.d(TAG, "Request headers:")
                    request.headers.forEach { (name, value) ->
                        Log.d(TAG, "  $name: $value")
                    }

                    Log.d(TAG, "Executing request...")
                    val call = client.newCall(request)
                    Log.d(TAG, "Call created, executing...")
                    
                    val startTime = System.currentTimeMillis()
                    // execute() is blocking - this is what we want inside a coroutine
                    val response = call.execute()
                    val duration = System.currentTimeMillis() - startTime

                    Log.d(TAG, "Got response in ${duration}ms: ${response.code} ${response.message}")
                    if (!response.isSuccessful) {
                        val responseBody = response.body?.string() ?: "(no body)"
                        Log.e(TAG, "HTTP ${response.code}: $responseBody")
                        response.close()
                        delay(backoffDelay)
                        backoffDelay = min(backoffDelay * 2, maxBackoffDelay)
                        continue
                    }

                    // Reset backoff on successful connection
                    backoffDelay = 1000L
                    Log.i(TAG, "Connected successfully! Starting to process event stream...")

                    // This blocks until the connection drops
                    response.body?.use { body ->
                        Log.d(TAG, "Processing event stream...")
                        processEventStream(body.charStream().buffered())
                    }

                    Log.d(TAG, "Connection closed")

                } catch (e: CancellationException) {
                    Log.d(TAG, "Cancelled")
                    break
                } catch (e: java.net.SocketTimeoutException) {
                    Log.e(TAG, "Socket timeout: ${e.message}", e)
                } catch (e: java.net.UnknownHostException) {
                    Log.e(TAG, "Unknown host: ${e.message}", e)
                } catch (e: java.io.IOException) {
                    Log.e(TAG, "IO Exception: ${e.message}", e)
                } catch (e: Exception) {
                    Log.e(TAG, "Error: ${e.javaClass.simpleName} - ${e.message}", e)
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
        var lineCount = 0

        Log.d(TAG, "Starting to read event stream...")
        while (reader.readLine().also { line = it } != null) {
            val currentLine = line ?: continue
            lineCount++
            
            if (lineCount % 10 == 0) {
                Log.d(TAG, "Read $lineCount lines from stream...")
            }

            when {
                currentLine.startsWith("data:") -> {
                    if (buffer.isNotEmpty()) buffer.append("\n")
                    buffer.append(currentLine.substring(5).trim())
                    Log.d(TAG, "Received data line: ${currentLine.substring(5).trim()}")
                }
                currentLine.isEmpty() && buffer.isNotEmpty() -> {
                    try {
                        Log.d(TAG, "Processing complete message: ${buffer.toString()}")
                        val command = gson.fromJson(buffer.toString(), Command::class.java)
                        Log.i(TAG, "Parsed command: $command")
                        onCommand(command)
                    } catch (e: Exception) {
                        Log.e(TAG, "Parse error for buffer: ${buffer.toString()}, error: ${e.message}", e)
                    }
                    buffer.clear()
                }
                currentLine.startsWith(":") -> {
                    Log.v(TAG, "SSE comment: $currentLine")
                }
                // comments (lines starting with :) and other fields - ignore
            }
        }
        Log.d(TAG, "Event stream ended. Total lines read: $lineCount")
    }

    fun disconnect() {
        job?.cancel()
        job = null
    }
}
