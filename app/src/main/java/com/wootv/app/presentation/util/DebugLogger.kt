package com.wootv.app.presentation.util

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import org.json.JSONObject

/**
 * Debug Logger for stream-auto-pause debugging session
 * Reports player events to debug server
 */
object DebugLogger {
    private const val TAG = "DebugLogger"
    private const val DEBUG_SERVER = "http://localhost:9999"
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO)

    fun logPlayerState(
        channelName: String,
        isPlaying: Boolean,
        playbackState: Int,
        bufferedPercentage: Int,
        error: String? = null
    ) {
        val logEntry = JSONObject().apply {
            put("type", "player_state")
            put("channel", channelName)
            put("isPlaying", isPlaying)
            put("playbackState", playbackState)
            put("bufferedPercentage", bufferedPercentage)
            put("error", error ?: JSONObject.NULL)
            put("timestamp", System.currentTimeMillis())
        }
        sendLog(logEntry)
    }

    fun logBufferStatus(
        channelName: String,
        bufferedPosition: Long,
        bufferedDuration: Long,
        totalDuration: Long
    ) {
        val logEntry = JSONObject().apply {
            put("type", "buffer_status")
            put("channel", channelName)
            put("bufferedPosition", bufferedPosition)
            put("bufferedDuration", bufferedDuration)
            put("totalDuration", totalDuration)
            put("timestamp", System.currentTimeMillis())
        }
        sendLog(logEntry)
    }

    fun logPlayerError(
        channelName: String,
        errorMessage: String,
        errorStack: String? = null
    ) {
        val logEntry = JSONObject().apply {
            put("type", "player_error")
            put("channel", channelName)
            put("errorMessage", errorMessage)
            put("errorStack", errorStack ?: JSONObject.NULL)
            put("timestamp", System.currentTimeMillis())
        }
        sendLog(logEntry)
    }

    fun logEvent(
        eventName: String,
        properties: Map<String, Any?>
    ) {
        val logEntry = JSONObject().apply {
            put("type", "event")
            put("event", eventName)
            put("properties", JSONObject(properties.filterValues { it != null }))
            put("timestamp", System.currentTimeMillis())
        }
        sendLog(logEntry)
    }

    private fun sendLog(logEntry: JSONObject) {
        scope.launch {
            try {
                val request = Request.Builder()
                    .url("$DEBUG_SERVER/log")
                    .post(logEntry.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "Failed to send log: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Debug server not available, logging locally: ${logEntry.toString()}")
            }
        }
    }
}
