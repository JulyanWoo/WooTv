package com.wootv.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wootv.app.data.local.entity.EpgProgramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpgDao {
    @Query("SELECT * FROM epg_programs WHERE channelId = :channelId AND endTime > :currentTime ORDER BY startTime LIMIT :limit")
    fun getProgramsForChannel(channelId: String, currentTime: Long, limit: Int = 20): Flow<List<EpgProgramEntity>>

    @Query("SELECT * FROM epg_programs WHERE channelId = :channelId AND startTime <= :currentTime AND endTime > :currentTime")
    suspend fun getCurrentProgram(channelId: String, currentTime: Long): EpgProgramEntity?

    @Query("SELECT * FROM epg_programs WHERE startTime >= :from AND endTime <= :to ORDER BY startTime")
    suspend fun getProgramsInRange(from: Long, to: Long): List<EpgProgramEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(programs: List<EpgProgramEntity>)

    @Query("DELETE FROM epg_programs WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylist(playlistId: Long)

    @Query("DELETE FROM epg_programs WHERE endTime < :before")
    suspend fun deleteOlderThan(before: Long)
}
