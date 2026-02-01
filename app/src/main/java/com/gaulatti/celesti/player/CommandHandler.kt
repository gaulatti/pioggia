package com.gaulatti.celesti.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.gaulatti.celesti.model.Command

/**
 * Handles playback commands: YouTube via intent, HLS/m3u via ExoPlayer.
 */
class CommandHandler(private val context: Context) {
    
    private val TAG = "CommandHandler"
    private var exoPlayer: ExoPlayer? = null
    
    /**
     * Handle a command from the SSE stream.
     */
    fun handle(command: Command) {
        when (command.type) {
            "youtube" -> handleYouTube(command.videoId)
            "m3u" -> handleM3U(command.url)
            "stop" -> handleStop()
            "heartbeat" -> handleHeartbeat()
            else -> Log.w(TAG, "Unknown command type: ${command.type}")
        }
    }
    
    /**
     * Handle YouTube playback command.
     * Opens YouTube app or browser with the video ID.
     */
    private fun handleYouTube(videoId: String?) {
        if (videoId.isNullOrBlank()) {
            Log.e(TAG, "YouTube command missing videoId")
            return
        }
        
        try {
            Log.d(TAG, "Playing YouTube video: $videoId")
            val youtubeUrl = "https://youtube.com/watch?v=$videoId"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start YouTube playback", e)
        }
    }
    
    /**
     * Handle m3u/HLS stream playback command.
     * Uses ExoPlayer to play the stream.
     */
    private fun handleM3U(url: String?) {
        if (url.isNullOrBlank()) {
            Log.e(TAG, "M3U command missing url")
            return
        }
        
        try {
            Log.d(TAG, "Playing m3u stream: $url")
            
            // Stop any existing playback
            stopExoPlayer()
            
            // Create new ExoPlayer instance
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                // Create media item from URL
                val mediaItem = MediaItem.fromUri(url)
                setMediaItem(mediaItem)
                
                // Prepare and play
                prepare()
                playWhenReady = true
            }
            
            Log.d(TAG, "ExoPlayer started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start m3u playback", e)
        }
    }
    
    /**
     * Handle stop command.
     * Stops and releases ExoPlayer.
     */
    private fun handleStop() {
        Log.d(TAG, "Stopping playback")
        stopExoPlayer()
    }
    
    /**
     * Handle heartbeat command.
     * No-op, just for connection keep-alive.
     */
    private fun handleHeartbeat() {
        Log.d(TAG, "Heartbeat received")
        // No action needed
    }
    
    /**
     * Stop and release ExoPlayer if active.
     */
    private fun stopExoPlayer() {
        exoPlayer?.let { player ->
            Log.d(TAG, "Releasing ExoPlayer")
            player.stop()
            player.release()
        }
        exoPlayer = null
    }
    
    /**
     * Release all resources.
     * Should be called when the service is destroyed.
     */
    fun release() {
        Log.d(TAG, "Releasing CommandHandler resources")
        stopExoPlayer()
    }
}
