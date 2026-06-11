package com.wootv.app.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wootv.app.data.local.entity.PlaylistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY id DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: PlaylistEntity): Long

    @Delete
    suspend fun delete(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun updateName(id: Long, name: String)

    @Query("UPDATE playlists SET lastRefreshed = :timestamp WHERE id = :id")
    suspend fun updateLastRefreshed(id: Long, timestamp: Long)
}
