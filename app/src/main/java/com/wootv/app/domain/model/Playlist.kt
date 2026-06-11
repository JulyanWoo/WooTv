package com.wootv.app.domain.model

data class Playlist(
    val id: Long = 0,
    val name: String,
    val url: String,
    val epgUrl: String? = null,
    val lastRefreshed: Long? = null
)
