package com.wootv.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Downloader @Inject constructor() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun download(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "WooTv/1.0")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Download failed: HTTP ${response.code}")
            }
            response.body?.string() ?: throw Exception("Empty response body")
        }
    }
}
