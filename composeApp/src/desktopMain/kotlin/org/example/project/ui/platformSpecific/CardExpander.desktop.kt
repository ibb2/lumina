package org.example.project.ui.platformSpecific

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.konyaco.fluent.component.CardExpanderItem
import com.konyaco.fluent.component.Icon
import com.konyaco.fluent.component.Text

@Composable
actual fun PlatformSpecificCardExpander(
    heading: String,
    caption: String,
    icon: ImageVector?,
    trailing: @Composable () -> Unit
) {
    CardExpanderItem(
        heading = { Text(heading) },
        caption = { Text(caption) },
        icon = {
            if (icon != null) {
                Icon(icon, null)
            }
        },
        trailing = { trailing() }
    )
}