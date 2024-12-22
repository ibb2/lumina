package org.example.project.ui.platformSpecific.buttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
expect fun PlatformSpecificIconButton(name: String, description: String, onClick: () -> Unit)