package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.data.download.TrackDownloadWorker
import com.example.data.local.SwarDatabase
import com.example.data.local.entity.DownloadStatus
import com.example.data.local.entity.DownloadedTrackEntity
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.text.DecimalFormat

interface DownloadsRepository {
    fun getAllDownloadedTracks(): Flow<List<DownloadedTrackEntity>>
    fun getCompletedDownloadedTracks(): Flow<List<DownloadedTrackEntity>>
    fun getDownloadedTrack(trackId: String): Flow<DownloadedTrackEntity?>
    fun isTrackDownloaded(trackId: String): Flow<Boolean>
    fun getTotalStorageSizeBytes(): Flow<Long>
    suspend fun getLocalTrackFile(trackId: String): File?
    fun downloadTrack(track: Track)
    suspend fun deleteDownloadedTrack(trackId: String): Result<Unit>
    suspend fun deleteAllDownloadedTracks(): Result<Unit>
    fun formatStorageSize(bytes: Long): String
}

class DownloadsRepositoryImpl(
    private val context: Context,
    private val database: SwarDatabase = SwarDatabase.getInstance(context)
) : DownloadsRepository {

    companion object {
        private const val TAG = "DownloadsRepository"
    }

    private val dao = database.downloadedTrackDao()
    private val settingsPreferences = com.example.data.preferences.SettingsPreferences(context)
    private val repositoryScope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
    
    private val workManager: WorkManager? by lazy {
        try {
            WorkManager.getInstance(context)
        } catch (e: Exception) {
            try {
                val config = Configuration.Builder()
                    .setMinimumLoggingLevel(Log.WARN)
                    .build()
                WorkManager.initialize(context, config)
                WorkManager.getInstance(context)
            } catch (e2: Exception) {
                Log.w(TAG, "WorkManager initialization fallback handled: ${e2.message}")
                null
            }
        }
    }

    init {
        // Reconcile downloads state on startup (cleanup ghost downloading states if file doesn't exist)
        repositoryScope.launch {
            try {
                val allTracks = dao.getAllDownloadedTracks().first()
                for (entity in allTracks) {
                    if (entity.status == DownloadStatus.COMPLETED) {
                        val file = File(entity.localFilePath)
                        if (!file.exists() || file.length() == 0L) {
                            // Check alternative name in downloads dir
                            val downloadsDir = File(context.filesDir, "downloads")
                            val cleanName = "${entity.trackId.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.m4a"
                            val altFile = File(downloadsDir, cleanName)
                            if (altFile.exists() && altFile.length() > 0L) {
                                dao.markCompleted(
                                    trackId = entity.trackId,
                                    fileSizeBytes = altFile.length(),
                                    localFilePath = altFile.absolutePath,
                                    downloadedAt = entity.downloadedAt
                                )
                            }
                        }
                    } else if (entity.status == DownloadStatus.DOWNLOADING || entity.status == DownloadStatus.PENDING) {
                        // Check if file actually exists and completed
                        val file = File(entity.localFilePath)
                        if (file.exists() && file.length() > 0L) {
                            dao.markCompleted(
                                trackId = entity.trackId,
                                fileSizeBytes = file.length(),
                                localFilePath = file.absolutePath,
                                downloadedAt = System.currentTimeMillis()
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Startup download reconciliation skipped: ${e.message}")
            }
        }
    }

    override fun getAllDownloadedTracks(): Flow<List<DownloadedTrackEntity>> {
        return dao.getAllDownloadedTracks()
    }

    override fun getCompletedDownloadedTracks(): Flow<List<DownloadedTrackEntity>> {
        return dao.getCompletedDownloadedTracks()
    }

    override fun getDownloadedTrack(trackId: String): Flow<DownloadedTrackEntity?> {
        return dao.getDownloadedTrackFlow(trackId)
    }

    override fun isTrackDownloaded(trackId: String): Flow<Boolean> {
        return dao.getDownloadedTrackFlow(trackId).map { entity ->
            entity?.status == DownloadStatus.COMPLETED && entity.localFilePath.isNotBlank() && File(entity.localFilePath).exists()
        }
    }

    override fun getTotalStorageSizeBytes(): Flow<Long> {
        return dao.getAllDownloadedTracks().map { list ->
            list.filter { it.status == DownloadStatus.COMPLETED }.sumOf { it.fileSizeBytes }
        }
    }

    override suspend fun getLocalTrackFile(trackId: String): File? = withContext(Dispatchers.IO) {
        if (trackId.isBlank()) return@withContext null

        try {
            // 1. Direct DB query for completed download record
            var entity = dao.getDownloadedTrack(trackId)
            if (entity == null && (trackId.contains("?") || trackId.contains("&") || trackId.contains("="))) {
                val cleanId = trackId.substringAfterLast("v=").substringBefore("&").substringBefore("?")
                if (cleanId.isNotBlank() && cleanId != trackId) {
                    entity = dao.getDownloadedTrack(cleanId)
                }
            }

            if (entity != null && entity.status == DownloadStatus.COMPLETED && entity.localFilePath.isNotBlank()) {
                val file = File(entity.localFilePath)
                if (file.exists() && file.length() > 0) {
                    Log.i(TAG, "[LocalFileCheck:FOUND_DB] Track '$trackId' local file verified at ${file.absolutePath} (size: ${file.length()} bytes)")
                    return@withContext file
                } else {
                    Log.w(TAG, "[LocalFileCheck:FILE_MISSING] DB entry found for '$trackId', but file at ${entity.localFilePath} is missing or 0 bytes")
                }
            }

            // 2. Fallback: check downloads directory on disk directly
            val downloadsDir = File(context.filesDir, "downloads")
            if (downloadsDir.exists()) {
                val cleanId = trackId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val directFileM4a = File(downloadsDir, "$cleanId.m4a")
                if (directFileM4a.exists() && directFileM4a.length() > 0) {
                    Log.i(TAG, "[LocalFileCheck:FOUND_DISK_M4A] Found direct file on disk for '$trackId' at ${directFileM4a.absolutePath} (size: ${directFileM4a.length()} bytes)")
                    return@withContext directFileM4a
                }

                val directMp3 = File(downloadsDir, "$cleanId.mp3")
                if (directMp3.exists() && directMp3.length() > 0) {
                    Log.i(TAG, "[LocalFileCheck:FOUND_DISK_MP3] Found direct file on disk for '$trackId' at ${directMp3.absolutePath} (size: ${directMp3.length()} bytes)")
                    return@withContext directMp3
                }

                val directOpus = File(downloadsDir, "$cleanId.opus")
                if (directOpus.exists() && directOpus.length() > 0) {
                    Log.i(TAG, "[LocalFileCheck:FOUND_DISK_OPUS] Found direct file on disk for '$trackId' at ${directOpus.absolutePath} (size: ${directOpus.length()} bytes)")
                    return@withContext directOpus
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[LocalFileCheck:ERROR] Failed checking local file for $trackId: ${e.message}", e)
        }

        Log.d(TAG, "[LocalFileCheck:NOT_FOUND] No local downloaded file exists for '$trackId'")
        null
    }

    override fun downloadTrack(track: Track) {
        if (track.id.isBlank()) return

        repositoryScope.launch {
            try {
                val existing = dao.getDownloadedTrack(track.id)
                // If already completed and file exists, skip
                if (existing != null && existing.status == DownloadStatus.COMPLETED && File(existing.localFilePath).exists()) {
                    Log.d(TAG, "Track '${track.title}' (${track.id}) is already downloaded, skipping")
                    return@launch
                }

                // If already downloading or pending, skip
                if (existing != null && (existing.status == DownloadStatus.DOWNLOADING || existing.status == DownloadStatus.PENDING)) {
                    Log.d(TAG, "Track '${track.title}' (${track.id}) is already being downloaded, skipping duplicate")
                    return@launch
                }

                val downloadsDir = File(context.filesDir, "downloads").apply {
                    if (!exists()) mkdirs()
                }
                val targetFile = File(downloadsDir, "${track.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.m4a")

                // Upsert initial downloading record
                dao.insertOrUpdate(
                    DownloadedTrackEntity(
                        trackId = track.id,
                        title = track.title,
                        artist = track.artist,
                        durationSeconds = track.durationSeconds,
                        thumbnailUrl = track.thumbnailUrl,
                        localFilePath = targetFile.absolutePath,
                        fileSizeBytes = 0L,
                        status = DownloadStatus.DOWNLOADING,
                        progress = 0
                    )
                )

                val inputData = workDataOf(
                    TrackDownloadWorker.KEY_TRACK_ID to track.id,
                    TrackDownloadWorker.KEY_TITLE to track.title,
                    TrackDownloadWorker.KEY_ARTIST to track.artist,
                    TrackDownloadWorker.KEY_DURATION to track.durationSeconds,
                    TrackDownloadWorker.KEY_THUMBNAIL_URL to track.thumbnailUrl
                )

                // Read unmetered / Wi-Fi requirement preference safely
                val requireWifi = try {
                    withTimeout(150L) {
                        settingsPreferences.downloadOnlyOnWifi.first()
                    }
                } catch (_: Exception) {
                    false
                }

                val networkType = if (requireWifi) NetworkType.UNMETERED else NetworkType.CONNECTED
                Log.i(TAG, "[DownloadEnqueue] TrackId='${track.id}', Title='${track.title}', WifiOnly=$requireWifi, NetworkType=$networkType")

                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(networkType)
                    .build()

                val downloadRequest = OneTimeWorkRequestBuilder<TrackDownloadWorker>()
                    .setInputData(inputData)
                    .setConstraints(constraints)
                    .addTag("download_track")
                    .addTag("download_${track.id}")
                    .build()

                workManager?.enqueueUniqueWork(
                    "download_${track.id}",
                    ExistingWorkPolicy.KEEP,
                    downloadRequest
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed enqueuing download: ${e.message}")
            }
        }
    }

    override suspend fun deleteDownloadedTrack(trackId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            workManager?.cancelUniqueWork("download_$trackId")
            val entity = dao.getDownloadedTrack(trackId)
            if (entity != null) {
                val file = File(entity.localFilePath)
                if (file.exists()) {
                    file.delete()
                }
            }
            // Also cleanup matching files in downloads dir
            val downloadsDir = File(context.filesDir, "downloads")
            val cleanName = "${trackId.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.m4a"
            val directFile = File(downloadsDir, cleanName)
            if (directFile.exists()) {
                directFile.delete()
            }

            dao.deleteByTrackId(trackId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAllDownloadedTracks(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            workManager?.cancelAllWorkByTag("download_track")
            val downloadsDir = File(context.filesDir, "downloads")
            if (downloadsDir.exists()) {
                downloadsDir.listFiles()?.forEach { it.delete() }
            }
            dao.deleteAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun formatStorageSize(bytes: Long): String {
        if (bytes <= 0) return "0 KB"
        val df = DecimalFormat("#.#")
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1.0 -> "${df.format(gb)} GB"
            mb >= 1.0 -> "${df.format(mb)} MB"
            else -> "${df.format(kb)} KB"
        }
    }
}
