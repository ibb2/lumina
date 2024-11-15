package org.example.project.sqldelight

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.project.database.LuminaDatabase

actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LuminaDatabase.Schema.create(driver)
        return driver
    }
}
