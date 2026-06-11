package com.wootv.app.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Downloader @Inject constructor(
    private val assetManager: android.content.res.AssetManager
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // Download as File (for M3U playlists - large files)
    suspend fun downloadAsFile(url: String): File = withContext(Dispatchers.IO) {
        Log.d("Downloader", "Downloading as file: $url")
        try {
            if (url.startsWith("asset://")) {
                val fileName = url.removePrefix("asset://")
                Log.d("Downloader", "Reading asset: $fileName")
                try {
                    val tempFile = File.createTempFile("playlist", ".m3u")
                    tempFile.deleteOnExit()
                    
                    assetManager.open(fileName).use { inputStream ->
                        FileOutputStream(tempFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    
                    Log.d("Downloader", "Asset saved to temp file: ${tempFile.length()} bytes")
                    tempFile
                } catch (e: Exception) {
                    Log.e("Downloader", "Failed to read asset: $fileName - ${e.message}")
                    throw Exception("Failed to read asset: $fileName - ${e.message}")
                }
            } else {
                Log.d("Downloader", "Downloading from URL: $url")
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "WooTv/1.0")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("Downloader", "HTTP error: ${response.code}")
                        throw Exception("Download failed: HTTP ${response.code}")
                    }
                    
                    val tempFile = File.createTempFile("playlist", ".m3u")
                    tempFile.deleteOnExit()
                    
                    response.body?.byteStream()?.use { inputStream ->
                        FileOutputStream(tempFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    } ?: throw Exception("Empty response body")
                    
                    Log.d("Downloader", "Downloaded to temp file: ${tempFile.length()} bytes")
                    tempFile
                }
            }
        } catch (e: Exception) {
            Log.e("Downloader", "Download error: ${e.message}")
            throw e
        }
    }

    // Download as String (for small files like EPG XML)
    suspend fun download(url: String): String = withContext(Dispatchers.IO) {
        Log.d("Downloader", "Downloading as string: $url")
        try {
            if (url.startsWith("asset://")) {
                val fileName = url.removePrefix("asset://")
                Log.d("Downloader", "Reading asset: $fileName")
                try {
                    assetManager.open(fileName).bufferedReader().use { reader ->
                        reader.readText()
                    }
                } catch (e: Exception) {
                    Log.e("Downloader", "Failed to read asset: $fileName - ${e.message}")
                    throw Exception("Failed to read asset: $fileName - ${e.message}")
                }
            } else {
                Log.d("Downloader", "Downloading from URL: $url")
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "WooTv/1.0")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("Downloader", "HTTP error: ${response.code}")
                        throw Exception("Download failed: HTTP ${response.code}")
                    }
                    response.body?.string() ?: throw Exception("Empty response body")
                }
            }
        } catch (e: Exception) {
            Log.e("Downloader", "Download error: ${e.message}")
            throw e
        }
    }
}
