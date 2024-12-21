package org.example.project.ui.platformSpecific

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.konyaco.fluent.Background
import com.konyaco.fluent.FluentTheme
import com.konyaco.fluent.LocalContentColor
import com.konyaco.fluent.background.Mica
import com.konyaco.fluent.surface.Card
//import com.konyaco.fluent.
import org.example.project.shared.data.EmailsDAO
import org.intellij.lang.annotations.JdkConstants.HorizontalAlignment



@Composable
actual fun PlatformSpecificCard(content: @Composable () -> Unit) {

    Card(modifier = Modifier) {
        Mica(modifier = Modifier) {
            content()
        }
    }
}