package com.wootv.app.data.local.mapper

import com.wootv.app.data.local.entity.PlaylistEntity
import com.wootv.app.domain.model.Playlist

fun PlaylistEntity.toDomain(): Playlist = Playlist(
    id = id,
    name = name,
    url = url,
    epgUrl = epgUrl,
    lastRefreshed = lastRefreshed
)

fun Playlist.toEntity(): PlaylistEntity = PlaylistEntity(
    id = id,
    name = name,
    url = url,
    epgUrl = epgUrl,
    lastRefreshed = lastRefreshed
)
