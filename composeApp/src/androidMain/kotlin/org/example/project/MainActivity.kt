package org.example.project

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.sqldelight.DatabaseDriverFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            App(emailService = EmailService(), DatabaseDriverFactory(this).create())
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    val context = currentCompositionLocalContext as Context
    App(emailService = EmailService(), driver = DatabaseDriverFactory(context = context).create())
}