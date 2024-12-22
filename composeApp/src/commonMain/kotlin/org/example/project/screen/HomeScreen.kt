package org.example.project.screen

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen

data class HomeScreen(val homepageContent: Unit): Screen {

    @Composable
    override fun Content() {
        homepageContent
    }
}