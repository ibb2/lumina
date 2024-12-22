package org.example.project.ui.platformSpecific.buttons

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.konyaco.fluent.icons.Icons
import com.konyaco.fluent.icons.regular.ArrowLeft
import com.konyaco.fluent.icons.regular.ArrowRight

@Composable
actual fun PlatformSpecificIconButton(
    name: String,
    description: String,
    onClick: () -> Unit
) {

    IconButton(onClick = onClick) {
        Icon(Icons.Default.ArrowLeft, contentDescription = "Back arrow")
    }
}