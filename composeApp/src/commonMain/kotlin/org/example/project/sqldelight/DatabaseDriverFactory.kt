package org.example.project.sqldelight

import app.cash.sqldelight.db.SqlDriver
import com.example.project.database.LuminaDatabase

expect class DatabaseDriverFactory {
    fun create(): SqlDriver
}

