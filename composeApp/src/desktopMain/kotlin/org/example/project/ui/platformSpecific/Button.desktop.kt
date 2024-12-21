package org.example.project.ui.platformSpecific

import androidx.compose.runtime.Composable
import com.konyaco.fluent.component.AccentButton
import com.konyaco.fluent.component.Button

@Composable
actual fun PlatformSpecificButton(onClick: () -> Unit,content: @Composable () -> Unit) {

    AccentButton(onClick = onClick) {
        content()
    }
}