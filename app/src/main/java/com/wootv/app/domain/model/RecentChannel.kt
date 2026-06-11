package com.wootv.app.domain.model

data class RecentChannel(
    val id: Long = 0,
    val channelId: Long,
    val lastWatchedAt: Long
)
