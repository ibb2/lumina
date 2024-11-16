package org.example.project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.project.database.LuminaDatabase
import org.example.project.sqldelight.DatabaseDriverFactory
import javax.xml.crypto.Data

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Lumina",
    ) {
        App(emailService = EmailService(), driver = DatabaseDriverFactory().create())
    }
}