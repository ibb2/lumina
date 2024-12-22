package org.example.project.ui.platformSpecific

import androidx.compose.material.Switch
import androidx.compose.runtime.Composable

@Composable
actual fun PlatformSpecificSwitch(state: Boolean, changed: () -> Unit) {

    Switch(checked = state, onCheckedChange = { changed() })
}