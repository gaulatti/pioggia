package com.gaulatti.celesti.model

/**
 * Data class representing a command received via SSE.
 *
 * @property type Command type: "youtube", "m3u", "stop", or "heartbeat"
 * @property videoId YouTube video ID (used when type is "youtube")
 * @property url Stream URL (used when type is "m3u")
 */
data class Command(
    val type: String,
    val videoId: String? = null,
    val url: String? = null
)
