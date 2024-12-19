package org.example.project.ui.platformSpecific

import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.konyaco.fluent.component.Button
import com.konyaco.fluent.component.ButtonColor
import com.konyaco.fluent.component.Icon
import com.konyaco.fluent.icons.Icons
import com.konyaco.fluent.icons.regular.Delete
import com.konyaco.fluent.icons.regular.Settings
import com.konyaco.fluent.scheme.VisualStateScheme
import com.sun.jna.platform.unix.X11.Visual

@Composable
actual fun PlatformSpecificDelete(modifier: Modifier, onClick: () -> Unit) {

    Button(
        onClick,
//        buttonColors = VisualStateScheme { ButtonColor(Color.Red, Color.White, Brush.sweepGradient()) },
        ) {
        Icon(Icons.Default.Delete, contentDescription = "Delete")
    }
}