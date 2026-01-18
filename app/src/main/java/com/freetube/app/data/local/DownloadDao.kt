package com.freetube.app.data.local

import androidx.room.*
import com.freetube.app.data.models.DownloadItem
import com.freetube.app.data.models.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    
    @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
    fun getAll(): Flow<List<DownloadItem>>
    
    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY downloadedAt DESC")
    fun getByStatus(status: DownloadStatus): Flow<List<DownloadItem>>
    
    @Query("SELECT * FROM downloads WHERE videoId = :videoId")
    suspend fun getById(videoId: String): DownloadItem?
    
    @Query("SELECT EXISTS(SELECT 1 FROM downloads WHERE videoId = :videoId AND status = 'COMPLETED')")
    suspend fun isDownloaded(videoId: String): Boolean
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DownloadItem)
    
    @Update
    suspend fun update(item: DownloadItem)
    
    @Delete
    suspend fun delete(item: DownloadItem)
    
    @Query("DELETE FROM downloads WHERE videoId = :videoId")
    suspend fun deleteById(videoId: String)
    
    @Query("UPDATE downloads SET status = :status WHERE videoId = :videoId")
    suspend fun updateStatus(videoId: String, status: DownloadStatus)
    
    @Query("SELECT COUNT(*) FROM downloads WHERE status = 'COMPLETED'")
    suspend fun getCompletedCount(): Int
}
