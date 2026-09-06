package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.DownloadedTrackDao
import com.example.data.local.dao.HistoryTrackDao
import com.example.data.local.dao.LikedSongDao
import com.example.data.local.dao.PlaylistDao
import com.example.data.local.dao.QuickSkipDao
import com.example.data.local.entity.DownloadedTrackEntity
import com.example.data.local.entity.HistoryTrackEntity
import com.example.data.local.entity.LikedSongEntity
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.QuickSkipEntity

@Database(
    entities = [
        DownloadedTrackEntity::class,
        LikedSongEntity::class,
        HistoryTrackEntity::class,
        PlaylistEntity::class,
        QuickSkipEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class SwarDatabase : RoomDatabase() {

    abstract fun downloadedTrackDao(): DownloadedTrackDao
    abstract fun likedSongDao(): LikedSongDao
    abstract fun historyTrackDao(): HistoryTrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun quickSkipDao(): QuickSkipDao

    companion object {
        @Volatile
        private var INSTANCE: SwarDatabase? = null

        fun getInstance(context: Context): SwarDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SwarDatabase::class.java,
                    "swar_music.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

