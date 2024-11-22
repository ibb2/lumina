package org.example.project.sqldelight

import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import com.example.Emails
import com.example.project.database.LuminaDatabase
import org.example.project.shared.AppModule

class DesktopAppModule : AppModule {

    private val db by lazy {
        LuminaDatabase(
            driver = DatabaseDriverFactory().create(),
            EmailsAdapter = Emails.Adapter(
                attachments_countAdapter = IntColumnAdapter,
            ),
        )
    }

    override fun provideAccountsDataSource(): AccountsDataSource {
        return AccountsDataSource(db)
    }

    override fun provideEmailsDataSource(): EmailsDataSource {
        return EmailsDataSource(db)
    }

    override fun provideAttachmentsDataSource(): AttachmentsDataSource {
        return AttachmentsDataSource(db)
    }
}