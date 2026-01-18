package com.freetube.app.data.local

import androidx.room.*
import com.freetube.app.data.models.WatchHistoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {
    
    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC")
    fun getAllHistory(): Flow<List<WatchHistoryItem>>
    
    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<WatchHistoryItem>>
    
    @Query("SELECT * FROM watch_history WHERE videoId = :videoId")
    suspend fun getById(videoId: String): WatchHistoryItem?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchHistoryItem)
    
    @Update
    suspend fun update(item: WatchHistoryItem)
    
    @Delete
    suspend fun delete(item: WatchHistoryItem)
    
    @Query("DELETE FROM watch_history WHERE videoId = :videoId")
    suspend fun deleteById(videoId: String)
    
    @Query("DELETE FROM watch_history")
    suspend fun clearAll()
    
    @Query("SELECT COUNT(*) FROM watch_history")
    suspend fun getCount(): Int
    
    @Query("UPDATE watch_history SET watchPosition = :position WHERE videoId = :videoId")
    suspend fun updateWatchPosition(videoId: String, position: Long)
}
