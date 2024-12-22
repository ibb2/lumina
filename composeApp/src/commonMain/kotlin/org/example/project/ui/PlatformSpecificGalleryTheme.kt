package org.example.project.ui

import androidx.compose.runtime.Composable

@Composable
expect fun GalleryTheme(displayMicaLayer:Boolean,darkMode: Boolean, useAcrylicPopup: Boolean, compactMode: Boolean, content: @Composable () -> Unit)