package com.freetube.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val channelId: String,
    val channelName: String,
    val avatarUrl: String,
    val subscribedAt: Long = System.currentTimeMillis()
)

@Dao
interface SubscriptionDao {
    
    @Query("SELECT * FROM subscriptions ORDER BY channelName ASC")
    fun getAll(): Flow<List<SubscriptionEntity>>
    
    @Query("SELECT * FROM subscriptions ORDER BY subscribedAt DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<SubscriptionEntity>>
    
    @Query("SELECT EXISTS(SELECT 1 FROM subscriptions WHERE channelId = :channelId)")
    suspend fun isSubscribed(channelId: String): Boolean
    
    @Query("SELECT EXISTS(SELECT 1 FROM subscriptions WHERE channelId = :channelId)")
    fun isSubscribedFlow(channelId: String): Flow<Boolean>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: SubscriptionEntity)
    
    @Query("DELETE FROM subscriptions WHERE channelId = :channelId")
    suspend fun delete(channelId: String)
    
    @Query("SELECT COUNT(*) FROM subscriptions")
    suspend fun getCount(): Int
}
