package org.example.project.ui.platformSpecific

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.konyaco.fluent.component.AutoSuggestBoxDefaults
import com.konyaco.fluent.component.TextBoxButton
import com.konyaco.fluent.component.TextBoxButtonDefaults
import com.konyaco.fluent.component.TextField
import com.konyaco.fluent.shape.FluentRoundedCornerShape

@Composable
actual fun PlatformSpecificTextField(
    modifier: Modifier,
    textFieldValue: TextFieldValue,
    updateTextFieldValue: (TextFieldValue) -> Unit
) {

    var searchQuery = remember { mutableStateOf("") }

    TextField(
        modifier = Modifier.border(BorderStroke(0.dp, Color.Transparent), CircleShape).fillMaxWidth(1f),
        value = textFieldValue,
        onValueChange = { updateTextFieldValue(it) },
        placeholder = { PlatformSpecificText("Search by body, subject, or sender") },
        trailing = {
            TextBoxButton(onClick = {}) { TextBoxButtonDefaults.SearchIcon() }
        },
        isClearable = true,
        shape = RoundedCornerShape(size = 16.dp),
    )

}