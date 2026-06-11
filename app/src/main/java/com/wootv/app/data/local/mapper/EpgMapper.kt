package com.wootv.app.data.local.mapper

import com.wootv.app.data.local.entity.EpgProgramEntity
import com.wootv.app.domain.model.EpgProgram

fun EpgProgramEntity.toDomain(): EpgProgram = EpgProgram(
    id = id,
    channelId = channelId,
    title = title,
    description = description,
    startTime = startTime,
    endTime = endTime
)

fun EpgProgram.toEntity(playlistId: Long): EpgProgramEntity = EpgProgramEntity(
    channelId = channelId,
    title = title,
    description = description,
    startTime = startTime,
    endTime = endTime,
    playlistId = playlistId
)
