package com.freetube.app.data.local

import androidx.room.*
import com.freetube.app.data.models.WatchLaterItem
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchLaterDao {
    
    @Query("SELECT * FROM watch_later ORDER BY addedAt DESC")
    fun getAll(): Flow<List<WatchLaterItem>>
    
    @Query("SELECT * FROM watch_later WHERE videoId = :videoId")
    suspend fun getById(videoId: String): WatchLaterItem?
    
    @Query("SELECT EXISTS(SELECT 1 FROM watch_later WHERE videoId = :videoId)")
    suspend fun exists(videoId: String): Boolean
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchLaterItem)
    
    @Delete
    suspend fun delete(item: WatchLaterItem)
    
    @Query("DELETE FROM watch_later WHERE videoId = :videoId")
    suspend fun deleteById(videoId: String)
    
    @Query("DELETE FROM watch_later")
    suspend fun clearAll()
    
    @Query("SELECT COUNT(*) FROM watch_later")
    suspend fun getCount(): Int
}
