package com.wootv.app.domain.model

data class Channel(
    val id: Long = 0,
    val playlistId: Long,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val groupTitle: String? = null,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val tvgLogo: String? = null,
    val isFavorite: Boolean = false
)
