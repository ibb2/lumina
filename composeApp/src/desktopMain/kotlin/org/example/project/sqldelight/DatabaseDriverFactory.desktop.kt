@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.example.project.sqldelight

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.project.database.LuminaDatabase
import java.io.File
import java.util.*

actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver {
        val dbFilePath: String = getPath(isDebug = false)
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:$dbFilePath", properties = Properties().apply{ put("foreign_keys", "true") })

        if (!File(dbFilePath).exists()) {
            LuminaDatabase.Schema.create(driver)
        }

        return driver
    }

    private fun getPath(isDebug: Boolean): String {
        val propertyKey = if (isDebug) "java.io.tmpdir" else "user.home"
        val parentFolderPath = System.getProperty(propertyKey) + "/SqlDelightDemo"
        val parentFolder = File(parentFolderPath)

        if (!parentFolder.exists()) {
            parentFolder.mkdirs()
        }

        val databasePath = File(parentFolderPath, "lumina.db")
        return databasePath.absolutePath
    }
}
