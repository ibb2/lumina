package org.example.project.ui.platformSpecific

import androidx.compose.runtime.Composable
import com.konyaco.fluent.component.Icon
import com.konyaco.fluent.component.SubtleButton
import com.konyaco.fluent.icons.Icons
import com.konyaco.fluent.icons.regular.Settings

@Composable
actual fun PlatformSpecificSettingsButton(onClick: () -> Unit) {

    SubtleButton(onClick) {
        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
    }
}