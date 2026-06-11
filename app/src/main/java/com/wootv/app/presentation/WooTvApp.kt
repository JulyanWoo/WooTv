package com.wootv.app.presentation

import androidx.compose.runtime.Composable
import com.wootv.app.presentation.navigation.TvNavHost
import com.wootv.app.presentation.theme.WooTvTheme

@Composable
fun WooTvApp() {
    WooTvTheme {
        TvNavHost()
    }
}
