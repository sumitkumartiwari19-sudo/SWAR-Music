package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing an implicit negative signal when a user quickly skips a track
 * (listened between 2 and 10 seconds specifically).
 */
@Entity(tableName = "quick_skips")
data class QuickSkipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackId: String,
    val title: String,
    val artist: String,
    val keywords: String,
    val listenDurationMs: Long,
    val skippedAt: Long = System.currentTimeMillis()
)
