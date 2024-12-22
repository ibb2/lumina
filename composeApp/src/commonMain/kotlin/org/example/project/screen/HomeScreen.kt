package org.example.project.screen

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow

data class HomeScreen(val homepageContent: @Composable (localNavigator: Navigator) -> Unit): Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        homepageContent(navigator)
    }
}

