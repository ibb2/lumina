package org.example.project.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import com.konyaco.fluent.*
import com.konyaco.fluent.background.Mica

@OptIn(ExperimentalFluentApi::class)
@Composable
actual fun GalleryTheme(
    displayMicaLayer: Boolean,
    darkMode: Boolean,
    useAcrylicPopup: Boolean,
    compactMode: Boolean,
    content: @Composable () -> Unit
) {
    FluentTheme(
        colors = if (darkMode) darkColors() else lightColors(),
        useAcrylicPopup = useAcrylicPopup,
        compactMode = compactMode
    ) {
        if (displayMicaLayer) {

            val gradient = if (darkMode) {
                listOf(
                    Color(0xff282C51),
                    Color(0xff2A344A),
                )
            } else {
                listOf(
                    Color(0xffB1D0ED),
                    Color(0xffDAE3EC),
                )
            }

            Mica(
                background = {
                    Image(
                        painter = BrushPainter(Brush.linearGradient(gradient)),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds
                    )
                },
                modifier = Modifier.fillMaxSize()
            ) {
                content()
            }
        } else {
            CompositionLocalProvider(
                LocalContentColor provides FluentTheme.colors.text.text.primary,
                content = content
            )
        }
    }

}