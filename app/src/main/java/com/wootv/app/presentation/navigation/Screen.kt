package com.wootv.app.presentation.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Home : Screen("home")
    data object PlaylistList : Screen("playlists")
    data object ChannelList : Screen("channels/{playlistId}") {
        fun createRoute(playlistId: Long) = "channels/$playlistId"
    }
    data object Player : Screen("player/{channelId}") {
        fun createRoute(channelId: Long) = "player/$channelId"
    }
    data object Epg : Screen("epg/{channelId}") {
        fun createRoute(channelId: Long) = "epg/$channelId"
    }
    data object Search : Screen("search")
}
