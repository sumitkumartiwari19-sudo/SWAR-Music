package com.example.data.download

import android.content.Context
import android.os.StatFs
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.local.SwarDatabase
import com.example.data.local.entity.DownloadStatus
import com.example.data.local.entity.DownloadedTrackEntity
import com.example.playback.extractor.AudioStreamResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

class TrackDownloadWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "TrackDownloadWorker"
        const val KEY_TRACK_ID = "key_track_id"
        const val KEY_TITLE = "key_title"
        const val KEY_ARTIST = "key_artist"
        const val KEY_DURATION = "key_duration"
        const val KEY_THUMBNAIL_URL = "key_thumbnail_url"
        const val MIN_STORAGE_REQUIRED_BYTES = 10L * 1024L * 1024L // 10 MB minimum free space
        private const val BUFFER_SIZE = 65536 // 64 KB high-throughput buffer
        private const val CHUNK_SIZE = 1024L * 1024L // 1 MB unthrottled range burst chunks
        private const val PROGRESS_THROTTLE_MS = 200L

        private val downloadHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
                .dispatcher(Dispatcher().apply {
                    maxRequests = 16
                    maxRequestsPerHost = 8
                })
                .build()
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val trackId = inputData.getString(KEY_TRACK_ID) ?: return@withContext Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: "Unknown Track"
        val artist = inputData.getString(KEY_ARTIST) ?: "Unknown Artist"
        val duration = inputData.getLong(KEY_DURATION, 0L)
        val thumbnailUrl = inputData.getString(KEY_THUMBNAIL_URL) ?: ""

        val database = SwarDatabase.getInstance(context)
        val dao = database.downloadedTrackDao()

        // 1. Prepare App-Private download directory
        val downloadsDir = File(context.filesDir, "downloads").apply {
            if (!exists()) mkdirs()
        }
        val cleanTrackId = trackId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        var targetFile = File(downloadsDir, "$cleanTrackId.m4a")
        val tempFile = File(downloadsDir, "${cleanTrackId}_${System.currentTimeMillis()}.tmp")

        // Clean up any stale temp files for this track
        downloadsDir.listFiles { file ->
            file.name.startsWith("${cleanTrackId}_") && file.name.endsWith(".tmp")
        }?.forEach { it.delete() }

        // 2. Storage space verification
        try {
            val statFs = StatFs(context.filesDir.path)
            val availableBytes = statFs.availableBlocksLong * statFs.blockSizeLong
            if (availableBytes < MIN_STORAGE_REQUIRED_BYTES) {
                Log.e(TAG, "Insufficient storage space: $availableBytes bytes available")
                dao.insertOrUpdate(
                    DownloadedTrackEntity(
                        trackId = trackId,
                        title = title,
                        artist = artist,
                        durationSeconds = duration,
                        thumbnailUrl = thumbnailUrl,
                        localFilePath = targetFile.absolutePath,
                        fileSizeBytes = 0L,
                        status = DownloadStatus.FAILED,
                        progress = 0,
                        errorMessage = "Insufficient storage space on device"
                    )
                )
                return@withContext Result.failure(
                    workDataOf("error" to "Insufficient storage space on device")
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "StatFs check warning: ${e.message}")
        }

        // 3. Mark status as DOWNLOADING in database
        dao.insertOrUpdate(
            DownloadedTrackEntity(
                trackId = trackId,
                title = title,
                artist = artist,
                durationSeconds = duration,
                thumbnailUrl = thumbnailUrl,
                localFilePath = targetFile.absolutePath,
                fileSizeBytes = 0L,
                status = DownloadStatus.DOWNLOADING,
                progress = 0
            )
        )
        Log.i(TAG, "[DownloadStart] TrackId='$trackId', Title='$title', Artist='$artist'")

        val startTimeMs = System.currentTimeMillis()

        try {
            // 4. Resolve audio stream URL with high bitrate & format metadata
            val resolver = AudioStreamResolver()
            val streamResult = resolver.resolveAudioStream(trackId)
            val extracted = streamResult.getOrNull()
                ?: throw IllegalStateException("Could not resolve audio stream URL for $trackId")

            val streamUrl = extracted.streamUrl
            val ext = when {
                extracted.format.contains("mp3", ignoreCase = true) -> "mp3"
                extracted.format.contains("opus", ignoreCase = true) -> "opus"
                else -> "m4a"
            }
            targetFile = File(downloadsDir, "$cleanTrackId.$ext")

            val userAgent = extracted.userAgent
                ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

            Log.i(
                TAG,
                "[DownloadStreamResolved] TrackId='$trackId', Bitrate=${extracted.bitrate} kbps, Format=${extracted.format}, Ext=$ext, Target='${targetFile.absolutePath}'"
            )

            // 5. Probe Content-Length and Range Support
            var totalContentLength = -1L
            var supportsRanges = false

            val probeRequest = Request.Builder()
                .url(streamUrl)
                .header("User-Agent", userAgent)
                .header("Range", "bytes=0-0")
                .header("Accept-Encoding", "identity")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            try {
                downloadHttpClient.newCall(probeRequest).execute().use { probeResponse ->
                    if (probeResponse.code == 206) {
                        supportsRanges = true
                        val contentRange = probeResponse.header("Content-Range")
                        if (contentRange != null && contentRange.contains("/")) {
                            totalContentLength = contentRange.substringAfter("/").trim().toLongOrNull() ?: -1L
                        }
                    } else if (probeResponse.isSuccessful) {
                        totalContentLength = probeResponse.header("Content-Length")?.toLongOrNull()
                            ?: probeResponse.body?.contentLength() ?: -1L
                    }
                }
            } catch (probeError: Exception) {
                Log.w(TAG, "Probe range check fallback: ${probeError.message}")
            }

            Log.i(
                TAG,
                "[DownloadProbe] TrackId='$trackId', SupportsRanges=$supportsRanges, TotalContentLength=$totalContentLength bytes"
            )

            var totalBytesDownloaded = 0L
            var lastReportedProgress = -1
            var lastProgressEmitTime = 0L

            if (supportsRanges && totalContentLength > 0) {
                // High-Speed Chunked Range Download (bypasses YouTube single-connection 30-50 KB/s stream rate limiter)
                var currentStart = 0L
                while (currentStart < totalContentLength) {
                    if (isStopped) {
                        tempFile.delete()
                        dao.deleteByTrackId(trackId)
                        Log.w(TAG, "[DownloadCancelled] TrackId='$trackId' during chunked download")
                        return@withContext Result.failure()
                    }

                    val currentEnd = minOf(currentStart + CHUNK_SIZE - 1, totalContentLength - 1)
                    val chunkRequest = Request.Builder()
                        .url(streamUrl)
                        .header("User-Agent", userAgent)
                        .header("Range", "bytes=$currentStart-$currentEnd")
                        .header("Accept-Encoding", "identity")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .build()

                    val chunkStartTime = System.currentTimeMillis()
                    val chunkResponse = downloadHttpClient.newCall(chunkRequest).execute()

                    if (!chunkResponse.isSuccessful && chunkResponse.code != 206) {
                        chunkResponse.close()
                        throw IllegalStateException("HTTP ${chunkResponse.code} error downloading byte range $currentStart-$currentEnd")
                    }

                    chunkResponse.body?.let { responseBody ->
                        responseBody.byteStream().buffered(BUFFER_SIZE).use { inputStream ->
                            FileOutputStream(tempFile, true).buffered(BUFFER_SIZE).use { outputStream ->
                                val buffer = ByteArray(BUFFER_SIZE)
                                var bytesRead: Int
                                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                    if (isStopped) {
                                        outputStream.flush()
                                        tempFile.delete()
                                        dao.deleteByTrackId(trackId)
                                        return@withContext Result.failure()
                                    }
                                    outputStream.write(buffer, 0, bytesRead)
                                    totalBytesDownloaded += bytesRead
                                }
                                outputStream.flush()
                            }
                        }
                    } ?: throw IllegalStateException("Empty chunk response body for range $currentStart-$currentEnd")

                    val chunkDurationMs = (System.currentTimeMillis() - chunkStartTime).coerceAtLeast(1L)
                    val chunkSizeDownloaded = (currentEnd - currentStart + 1)
                    val chunkSpeedKbps = (chunkSizeDownloaded * 1000L) / (chunkDurationMs * 1024L)

                    currentStart = currentEnd + 1

                    val now = System.currentTimeMillis()
                    val currentProgress = ((totalBytesDownloaded * 100) / totalContentLength).toInt().coerceIn(0, 99)
                    if (currentProgress > lastReportedProgress && (now - lastProgressEmitTime >= PROGRESS_THROTTLE_MS || currentProgress == 99)) {
                        lastReportedProgress = currentProgress
                        lastProgressEmitTime = now
                        setProgress(
                            workDataOf(
                                "progress" to currentProgress,
                                "speedKbps" to chunkSpeedKbps
                            )
                        )
                        dao.updateProgress(
                            trackId = trackId,
                            progress = currentProgress,
                            fileSizeBytes = totalBytesDownloaded,
                            status = DownloadStatus.DOWNLOADING
                        )
                        val speedMbps = String.format(Locale.US, "%.2f", chunkSpeedKbps / 1024.0)
                        Log.d(
                            TAG,
                            "[DownloadProgress] TrackId='$trackId', Progress=$currentProgress%, Speed=${chunkSpeedKbps} KB/s (${speedMbps} MB/s), TotalRead=$totalBytesDownloaded/$totalContentLength"
                        )
                    }
                }
            } else {
                // Direct High-Throughput Stream Download with 64KB Buffer
                val streamRequest = Request.Builder()
                    .url(streamUrl)
                    .header("User-Agent", userAgent)
                    .header("Range", "bytes=0-")
                    .header("Accept-Encoding", "identity")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .build()

                val streamResponse = downloadHttpClient.newCall(streamRequest).execute()
                if (!streamResponse.isSuccessful && streamResponse.code != 206) {
                    streamResponse.close()
                    throw IllegalStateException("HTTP error ${streamResponse.code} downloading audio stream")
                }

                val responseBody = streamResponse.body
                    ?: throw IllegalStateException("Response body is null")
                if (totalContentLength <= 0) {
                    totalContentLength = responseBody.contentLength()
                }

                responseBody.byteStream().buffered(BUFFER_SIZE).use { inputStream ->
                    FileOutputStream(tempFile).buffered(BUFFER_SIZE).use { outputStream ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int
                        var windowBytes = 0L
                        var windowStartTime = System.currentTimeMillis()

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            if (isStopped) {
                                outputStream.flush()
                                tempFile.delete()
                                dao.deleteByTrackId(trackId)
                                Log.w(TAG, "[DownloadCancelled] TrackId='$trackId'")
                                return@withContext Result.failure()
                            }

                            outputStream.write(buffer, 0, bytesRead)
                            totalBytesDownloaded += bytesRead
                            windowBytes += bytesRead

                            val now = System.currentTimeMillis()
                            if (now - lastProgressEmitTime >= PROGRESS_THROTTLE_MS) {
                                val windowDurationMs = (now - windowStartTime).coerceAtLeast(1L)
                                val currentSpeedKbps = (windowBytes * 1000L) / (windowDurationMs * 1024L)
                                windowBytes = 0L
                                windowStartTime = now
                                lastProgressEmitTime = now

                                val currentProgress = if (totalContentLength > 0) {
                                    ((totalBytesDownloaded * 100) / totalContentLength).toInt().coerceIn(0, 99)
                                } else {
                                    ((totalBytesDownloaded / (400 * 1024)).toInt()).coerceIn(1, 95)
                                }

                                lastReportedProgress = currentProgress
                                setProgress(
                                    workDataOf(
                                        "progress" to currentProgress,
                                        "speedKbps" to currentSpeedKbps
                                    )
                                )
                                dao.updateProgress(
                                    trackId = trackId,
                                    progress = currentProgress,
                                    fileSizeBytes = totalBytesDownloaded,
                                    status = DownloadStatus.DOWNLOADING
                                )
                            }
                        }
                        outputStream.flush()
                    }
                }
            }

            // 6. Verify temp file existence and size integrity
            if (!tempFile.exists() || tempFile.length() <= 0L) {
                throw IllegalStateException("Downloaded temporary file is missing or empty (0 bytes)")
            }

            if (totalContentLength > 0 && tempFile.length() < (totalContentLength * 0.90).toLong()) {
                throw IllegalStateException("Downloaded file size (${tempFile.length()}) is significantly smaller than content length ($totalContentLength)")
            }

            // 7. Atomic Move/Rename temp file to target persistent file
            if (targetFile.exists()) {
                targetFile.delete()
            }
            val renameSuccess = tempFile.renameTo(targetFile)
            if (!renameSuccess) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            // 8. Verify final persistent file on device
            if (!targetFile.exists() || targetFile.length() <= 0L) {
                throw IllegalStateException("Target persistent file validation failed at ${targetFile.absolutePath}")
            }

            val finalFileSize = targetFile.length()
            val totalElapsedSec = (System.currentTimeMillis() - startTimeMs) / 1000.0
            val avgSpeedKbps = if (totalElapsedSec > 0) (finalFileSize / 1024.0 / totalElapsedSec).toInt() else 0
            val avgSpeedMbps = String.format(Locale.US, "%.2f", avgSpeedKbps / 1024.0)

            // 9. Update Room database with final verified absolute path & non-zero file size
            dao.markCompleted(
                trackId = trackId,
                fileSizeBytes = finalFileSize,
                localFilePath = targetFile.absolutePath,
                downloadedAt = System.currentTimeMillis()
            )

            Log.i(
                TAG,
                "[DownloadComplete] SUCCESS! TrackId='$trackId', Title='$title', File='${targetFile.absolutePath}', Size=$finalFileSize bytes, TotalTime=${totalElapsedSec}s, AvgSpeed=${avgSpeedMbps} MB/s (${avgSpeedKbps} KB/s)"
            )

            Result.success(
                workDataOf(
                    "filePath" to targetFile.absolutePath,
                    "fileSize" to finalFileSize,
                    "speedKbps" to avgSpeedKbps
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "[DownloadFailed] TrackId='$trackId', Error='${e.message}'", e)
            if (tempFile.exists()) {
                tempFile.delete()
            }
            if (targetFile.exists() && targetFile.length() == 0L) {
                targetFile.delete()
            }
            dao.markFailed(
                trackId = trackId,
                errorMessage = e.localizedMessage ?: "Network or download error"
            )
            Result.failure(workDataOf("error" to (e.localizedMessage ?: "Unknown error")))
        }
    }
}

