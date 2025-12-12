package com.devrinth.launchpad.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for clipboard history operations.
 * All methods are suspend functions or return Flow for off-main-thread execution.
 */
@Dao
interface ClipboardDao {

    /**
     * Insert a new clipboard entry. Replaces on hash conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ClipboardEntity)

    /**
     * Get all clipboard entries ordered by most recent first.
     */
    @Query("SELECT * FROM clipboard_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ClipboardEntity>>

    /**
     * Get the most recent N clipboard entries.
     */
    @Query("SELECT * FROM clipboard_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<ClipboardEntity>>

    /**
     * Get recent entries as a one-shot list (for plugin queries).
     */
    @Query("SELECT * FROM clipboard_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentList(limit: Int): List<ClipboardEntity>

    /**
     * Delete entries older than the given timestamp, excluding pinned items.
     */
    @Query("DELETE FROM clipboard_history WHERE timestamp < :timestamp AND isPinned = 0")
    suspend fun deleteOlderThan(timestamp: Long)

    /**
     * Clear all clipboard history.
     */
    @Query("DELETE FROM clipboard_history")
    suspend fun deleteAll()

    /**
     * Check if an entry with the given hash already exists.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM clipboard_history WHERE contentHash = :hash)")
    suspend fun existsByHash(hash: String): Boolean

    /**
     * Toggle pin status for an entry.
     */
    @Query("UPDATE clipboard_history SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    /**
     * Get entry count for display purposes.
     */
    @Query("SELECT COUNT(*) FROM clipboard_history")
    suspend fun getCount(): Int

    /**
     * Search clipboard history by content.
     */
    @Query("SELECT * FROM clipboard_history WHERE content LIKE '%' || :query || '%' ORDER BY timestamp DESC LIMIT :limit")
    suspend fun search(query: String, limit: Int): List<ClipboardEntity>
}
