package com.wootv.app.domain.model

data class EpgProgram(
    val id: Long = 0,
    val channelId: String,
    val title: String,
    val description: String? = null,
    val startTime: Long,
    val endTime: Long
) {
    val isCurrent: Boolean
        get() {
            val now = System.currentTimeMillis()
            return startTime <= now && endTime > now
        }
}
