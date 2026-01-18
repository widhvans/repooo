package com.freetube.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long = System.currentTimeMillis()
)

@Dao
interface SearchHistoryDao {
    
    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT :limit")
    fun getRecent(limit: Int = 10): Flow<List<SearchHistoryEntity>>
    
    @Query("SELECT * FROM search_history WHERE query LIKE :prefix || '%' ORDER BY searchedAt DESC LIMIT :limit")
    suspend fun searchByPrefix(prefix: String, limit: Int = 5): List<SearchHistoryEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SearchHistoryEntity)
    
    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun delete(query: String)
    
    @Query("DELETE FROM search_history")
    suspend fun clearAll()
}
