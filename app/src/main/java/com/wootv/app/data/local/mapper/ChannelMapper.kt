package com.wootv.app.data.local.mapper

import com.wootv.app.data.local.entity.ChannelEntity
import com.wootv.app.domain.model.Channel

fun ChannelEntity.toDomain(): Channel = Channel(
    id = id,
    playlistId = playlistId,
    name = name,
    streamUrl = streamUrl,
    logoUrl = logoUrl,
    groupTitle = groupTitle,
    tvgId = tvgId,
    tvgName = tvgName,
    tvgLogo = tvgLogo,
    isFavorite = isFavorite
)

fun Channel.toEntity(): ChannelEntity = ChannelEntity(
    id = id,
    playlistId = playlistId,
    name = name,
    streamUrl = streamUrl,
    logoUrl = logoUrl,
    groupTitle = groupTitle,
    tvgId = tvgId,
    tvgName = tvgName,
    tvgLogo = tvgLogo,
    isFavorite = isFavorite
)
