package org.example.project.sqldelight

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
    fun create(): SqlDriver
}
