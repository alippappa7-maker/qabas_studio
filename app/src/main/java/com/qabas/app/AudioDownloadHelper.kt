package com.qabas.app

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object AudioDownloadHelper {

    /**
     * Downloads or exports an audio track directly to the device's public Downloads / Music folder.
     */
    suspend fun downloadAudio(context: Context, audioUrl: String, title: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val cleanTitle = title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { "qabas_audio" }
                val fileName = "${cleanTitle}.mp3"

                when {
                    audioUrl.startsWith("assets/") -> {
                        // Copy from APK assets to public downloads
                        val assetPath = audioUrl.removePrefix("assets/")
                        context.assets.open(assetPath).use { input ->
                            saveStreamToPublicStorage(context, input, fileName)
                        }
                    }
                    audioUrl.startsWith("http://") || audioUrl.startsWith("https://") -> {
                        // Remote download via DownloadManager or direct stream
                        runCatching {
                            val request = DownloadManager.Request(Uri.parse(audioUrl))
                                .setTitle(title)
                                .setDescription("جاري تنزيل المقطع الصوتي من قبس")
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                                .setAllowedOverMetered(true)
                                .setAllowedOverRoaming(true)

                            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                            dm?.enqueue(request)
                            true
                        }.getOrElse {
                            // Fallback to direct HTTP stream download if DownloadManager is restricted
                            val url = URL(audioUrl)
                            val conn = url.openConnection() as HttpURLConnection
                            conn.connectTimeout = 15000
                            conn.readTimeout = 30000
                            conn.inputStream.use { input ->
                                saveStreamToPublicStorage(context, input, fileName)
                            }
                        }
                    }
                    audioUrl.startsWith("/") -> {
                        val file = File(audioUrl)
                        if (file.exists()) {
                            file.inputStream().use { input ->
                                saveStreamToPublicStorage(context, input, fileName)
                            }
                        } else {
                            false
                        }
                    }
                    else -> {
                        try {
                            context.assets.open("audio/recitation_yusuf.mp3").use { input ->
                                saveStreamToPublicStorage(context, input, fileName)
                            }
                        } catch (_: Exception) {
                            false
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AudioDownloadHelper", "Download failed: ${e.message}", e)
                false
            }
        }
    }

    private fun saveStreamToPublicStorage(context: Context, inputStream: InputStream, fileName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "audio/mpeg")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { output ->
                        inputStream.copyTo(output)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    true
                } else {
                    false
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val targetFile = File(downloadsDir, fileName)
                FileOutputStream(targetFile).use { output ->
                    inputStream.copyTo(output)
                }
                MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf("audio/mpeg"), null)
                true
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioDownloadHelper", "saveStream failed: ${e.message}", e)
            false
        }
    }
}
