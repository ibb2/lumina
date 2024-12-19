package org.example.project.ui.platformSpecific

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue

@Composable
expect fun PlatformSpecificTextField(modifier: Modifier, textFieldValue: TextFieldValue, updateTextFieldValue: (TextFieldValue) -> Unit)