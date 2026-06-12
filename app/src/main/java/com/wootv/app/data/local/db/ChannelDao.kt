package com.wootv.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wootv.app.data.local.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE playlistId = :playlistId ORDER BY groupTitle, name")
    fun getChannelsByPlaylist(playlistId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels ORDER BY name")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun getChannelById(id: Long): ChannelEntity?

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND groupTitle = :group ORDER BY name")
    fun getChannelsByGroup(playlistId: Long, group: String): Flow<List<ChannelEntity>>

    @Query("SELECT DISTINCT groupTitle FROM channels WHERE playlistId = :playlistId AND groupTitle IS NOT NULL ORDER BY groupTitle")
    fun getGroupsByPlaylist(playlistId: Long): Flow<List<String>>

    @Query("SELECT * FROM channels WHERE name LIKE '%' || :query || '%' OR tvgName LIKE '%' || :query || '%'")
    fun searchChannels(query: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE isFavorite = 1")
    fun getFavoriteChannels(): Flow<List<ChannelEntity>>

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)

    @Query("SELECT streamUrl FROM channels WHERE playlistId = :playlistId AND isFavorite = 1")
    suspend fun getFavoriteStreamUrls(playlistId: Long): List<String>

    @Query("UPDATE channels SET isFavorite = 1 WHERE playlistId = :playlistId AND streamUrl IN (:streamUrls)")
    suspend fun restoreFavoritesByStreamUrl(playlistId: Long, streamUrls: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<ChannelEntity>)

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylist(playlistId: Long)
}
