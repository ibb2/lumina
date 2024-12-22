package org.example.project.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.example.project.ui.platformSpecific.PlatformSpecificButton
import org.example.project.ui.platformSpecific.PlatformSpecificSwitch
import org.example.project.ui.platformSpecific.PlatformSpecificText
import org.example.project.ui.platformSpecific.buttons.PlatformSpecificIconButton

data class SettingsScreen(val minimiseOnClose: Boolean = false): Screen {

    @Composable
    override fun Content() {
        settingsPage()
    }
}

@Composable
fun settingsPage() {
    val navigator = LocalNavigator.currentOrThrow

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        PlatformSpecificText(text = "Settings page",modifier = Modifier.padding(bottom = 24.dp), fontSize = 32)

        var minimise by remember { mutableStateOf(false) }

        Column(modifier = Modifier.padding(8.dp).border(2.dp, Color.DarkGray, RoundedCornerShape(0.5f))) {
            PlatformSpecificIconButton("Arrow back", "Arrow back", { navigator.pop()})

            Row {
                PlatformSpecificText(text = "Minimise",modifier = Modifier.padding(bottom = 18.dp))
                PlatformSpecificSwitch(minimise, { minimise = !minimise })
                PlatformSpecificButton(fun() {}) {
                    PlatformSpecificText(text = "On/Off")
                }
            }
        }
    }
}