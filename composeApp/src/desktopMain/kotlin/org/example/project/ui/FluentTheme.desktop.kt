package org.example.project.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.konyaco.fluent.FluentTheme
import com.konyaco.fluent.LocalContentColor
import com.konyaco.fluent.background.Mica
import com.konyaco.fluent.darkColors
import com.konyaco.fluent.lightColors
import org.jetbrains.skiko.SystemTheme
import org.jetbrains.skiko.currentSystemTheme

@Composable
actual fun FluentThemeEntry(content: @Composable () -> Unit) {

    FluentTheme(colors = if (currentSystemTheme == SystemTheme.DARK) darkColors() else lightColors()) {
        if (true) {
            val gradient = if (currentSystemTheme == SystemTheme.DARK) {
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
//                background = {
//                    Image(
//                        painter = BrushPainter(Brush.linearGradient(gradient)),
//                        contentDescription = null,
//                        contentScale = ContentScale.FillBounds
//                    )
//                },
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