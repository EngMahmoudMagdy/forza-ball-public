package com.forzaball.shared.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    SharedIosRootScreen()
}

@Composable
private fun SharedIosRootScreen() {
    MaterialTheme {
        KmpStatusCard(
            title = "ForzaBall Shared UI",
            body = "Compose Multiplatform screen from KMP framework is ready for iOS host.",
        )
    }
}
