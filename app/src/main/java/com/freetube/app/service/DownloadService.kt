package com.freetube.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.freetube.app.MainActivity
import com.freetube.app.R
import com.freetube.app.data.local.DownloadDao
import com.freetube.app.data.models.DownloadItem
import com.freetube.app.data.models.DownloadStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Service for downloading videos in the background
 */
@AndroidEntryPoint
class DownloadService : Service() {
    
    @Inject
    lateinit var downloadDao: DownloadDao
    
    @Inject
    lateinit var okHttpClient: OkHttpClient
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeDownloads = mutableMapOf<String, Job>()
    
    companion object {
        const val CHANNEL_ID = "freetube_downloads"
        const val NOTIFICATION_ID = 2
        
        const val ACTION_START_DOWNLOAD = "com.freetube.app.action.START_DOWNLOAD"
        const val ACTION_PAUSE_DOWNLOAD = "com.freetube.app.action.PAUSE_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.freetube.app.action.CANCEL_DOWNLOAD"
        
        const val EXTRA_VIDEO_ID = "extra_video_id"
        const val EXTRA_VIDEO_TITLE = "extra_video_title"
        const val EXTRA_DOWNLOAD_URL = "extra_download_url"
        const val EXTRA_THUMBNAIL_URL = "extra_thumbnail_url"
        const val EXTRA_CHANNEL_NAME = "extra_channel_name"
        const val EXTRA_DURATION = "extra_duration"
        const val EXTRA_QUALITY = "extra_quality"
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val videoId = intent.getStringExtra(EXTRA_VIDEO_ID) ?: return START_NOT_STICKY
                val title = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: "Video"
                val downloadUrl = intent.getStringExtra(EXTRA_DOWNLOAD_URL) ?: return START_NOT_STICKY
                val thumbnailUrl = intent.getStringExtra(EXTRA_THUMBNAIL_URL) ?: ""
                val channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: ""
                val duration = intent.getLongExtra(EXTRA_DURATION, 0)
                val quality = intent.getStringExtra(EXTRA_QUALITY) ?: "720p"
                
                startDownload(videoId, title, downloadUrl, thumbnailUrl, channelName, duration, quality)
            }
            ACTION_PAUSE_DOWNLOAD -> {
                val videoId = intent.getStringExtra(EXTRA_VIDEO_ID) ?: return START_NOT_STICKY
                pauseDownload(videoId)
            }
            ACTION_CANCEL_DOWNLOAD -> {
                val videoId = intent.getStringExtra(EXTRA_VIDEO_ID) ?: return START_NOT_STICKY
                cancelDownload(videoId)
            }
        }
        
        return START_NOT_STICKY
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Video download progress"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun startDownload(
        videoId: String,
        title: String,
        downloadUrl: String,
        thumbnailUrl: String,
        channelName: String,
        duration: Long,
        quality: String
    ) {
        // Create download directory
        val downloadDir = File(getExternalFilesDir(null), "downloads")
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        
        val outputFile = File(downloadDir, "${videoId}_${quality}.mp4")
        
        val job = serviceScope.launch {
            try {
                // Create download item
                val downloadItem = DownloadItem(
                    videoId = videoId,
                    title = title,
                    thumbnailUrl = thumbnailUrl,
                    channelName = channelName,
                    duration = duration,
                    filePath = outputFile.absolutePath,
                    fileSize = 0,
                    quality = quality,
                    status = DownloadStatus.DOWNLOADING
                )
                downloadDao.insert(downloadItem)
                
                // Start foreground
                startForeground(NOTIFICATION_ID, createDownloadNotification(title, 0))
                
                // Download file
                val request = Request.Builder()
                    .url(downloadUrl)
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val body = response.body ?: throw Exception("Empty response")
                    val contentLength = body.contentLength()
                    
                    body.byteStream().use { input ->
                        FileOutputStream(outputFile).use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalBytesRead: Long = 0
                            var lastProgress = 0
                            
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                if (!isActive) {
                                    // Download cancelled
                                    output.close()
                                    outputFile.delete()
                                    return@launch
                                }
                                
                                output.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead
                                
                                // Update progress
                                if (contentLength > 0) {
                                    val progress = (totalBytesRead * 100 / contentLength).toInt()
                                    if (progress > lastProgress) {
                                        lastProgress = progress
                                        updateNotification(title, progress)
                                    }
                                }
                            }
                        }
                    }
                    
                    // Download complete
                    downloadDao.updateStatus(videoId, DownloadStatus.COMPLETED)
                    downloadDao.update(
                        downloadItem.copy(
                            status = DownloadStatus.COMPLETED,
                            fileSize = outputFile.length()
                        )
                    )
                    
                } else {
                    throw Exception("Download failed: ${response.code}")
                }
                
            } catch (e: Exception) {
                downloadDao.updateStatus(videoId, DownloadStatus.FAILED)
                e.printStackTrace()
            } finally {
                activeDownloads.remove(videoId)
                if (activeDownloads.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
        
        activeDownloads[videoId] = job
    }
    
    private fun pauseDownload(videoId: String) {
        activeDownloads[videoId]?.cancel()
        serviceScope.launch {
            downloadDao.updateStatus(videoId, DownloadStatus.PAUSED)
        }
    }
    
    private fun cancelDownload(videoId: String) {
        activeDownloads[videoId]?.cancel()
        serviceScope.launch {
            downloadDao.deleteById(videoId)
        }
    }
    
    private fun createDownloadNotification(title: String, progress: Int): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Downloading")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }
    
    private fun updateNotification(title: String, progress: Int) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createDownloadNotification(title, progress))
    }
    
    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
