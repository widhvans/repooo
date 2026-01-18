package com.freetube.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.freetube.app.data.models.DownloadItem
import com.freetube.app.data.models.WatchHistoryItem
import com.freetube.app.data.models.WatchLaterItem

@Database(
    entities = [
        WatchHistoryItem::class,
        WatchLaterItem::class,
        DownloadItem::class,
        SearchHistoryEntity::class,
        SubscriptionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun watchLaterDao(): WatchLaterDao
    abstract fun downloadDao(): DownloadDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun subscriptionDao(): SubscriptionDao
}
