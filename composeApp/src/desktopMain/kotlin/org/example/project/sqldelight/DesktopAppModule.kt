package org.example.project.sqldelight

import com.example.project.database.LuminaDatabase
import org.example.project.shared.AppModule

class DesktopAppModule: AppModule {

    private val db by lazy {
        LuminaDatabase(
            driver = DatabaseDriverFactory().create()
        )
    }

    override fun provideAccountDataSource(): AccountDataSource {
        return AccountDataSource(db)
    }
}