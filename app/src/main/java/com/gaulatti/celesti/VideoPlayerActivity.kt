package com.gaulatti.celesti

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class VideoPlayerActivity : Activity() {
    
    private val TAG = "VideoPlayerActivity"
    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "Activity created")
        
        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Hide system UI for immersive experience
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        )
        
        // Create PlayerView
        playerView = PlayerView(this).apply {
            useController = false  // No playback controls for TV
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
        }
        setContentView(playerView)
        
        // Initialize player
        initializePlayer()
        
        // Get video URL from intent
        handleIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "New intent received")
        setIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent?) {
        val videoUrl = intent?.getStringExtra(EXTRA_VIDEO_URL)
        if (videoUrl != null) {
            Log.d(TAG, "Loading video: $videoUrl")
            playVideo(videoUrl)
        } else {
            Log.e(TAG, "No video URL in intent")
            finish()
        }
    }
    
    private fun initializePlayer() {
        if (player == null) {
            player = ExoPlayer.Builder(this).build().also { exoPlayer ->
                playerView?.player = exoPlayer
                
                // Close activity when playback ends
                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            Log.d(TAG, "Playback ended, finishing activity")
                            finish()
                        }
                    }
                })
            }
        }
    }
    
    private fun playVideo(url: String) {
        player?.let { exoPlayer ->
            val mediaItem = MediaItem.fromUri(url)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            Log.d(TAG, "Video playback started")
        }
    }
    
    override fun onPause() {
        super.onPause()
        player?.pause()
    }
    
    override fun onResume() {
        super.onResume()
        player?.play()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
        playerView = null
    }
    
    companion object {
        const val EXTRA_VIDEO_URL = "video_url"
    }
}
