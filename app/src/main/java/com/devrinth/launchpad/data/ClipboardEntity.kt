package com.devrinth.launchpad.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Room entity representing a clipboard history entry.
 *
 * @property id Unique identifier (auto-generated)
 * @property content Full clipboard text content
 * @property contentHash MD5 hash for duplicate detection
 * @property timestamp Epoch millis when content was copied
 * @property type Content type (e.g., "text/plain")
 * @property isPinned Pinned items survive automatic cleanup
 * @property preview Truncated content for display (max 100 chars)
 */
@Entity(
    tableName = "clipboard_history",
    indices = [Index(value = ["contentHash"], unique = true)]
)
data class ClipboardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val contentHash: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "text/plain",
    val isPinned: Boolean = false,
    val preview: String = content.take(100)
)
