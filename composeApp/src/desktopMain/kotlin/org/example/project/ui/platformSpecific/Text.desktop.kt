package org.example.project.ui.platformSpecific

import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.konyaco.fluent.Background
import com.konyaco.fluent.FluentTheme
import com.konyaco.fluent.LocalContentColor
import com.konyaco.fluent.background.Layer
import com.konyaco.fluent.background.Mica
import com.konyaco.fluent.component.Text


@Composable
actual fun PlatformSpecificText(text: String, modifier: Modifier?, fontSize: Int) {
    if (modifier != null) Text(modifier = Modifier, text = text, fontSize = fontSize.sp) else Text(text = text, fontSize = fontSize.sp)
}