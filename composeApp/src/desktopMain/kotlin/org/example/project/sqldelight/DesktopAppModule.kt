package org.example.project.sqldelight

import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import com.example.Attachments
import com.example.Emails
import com.example.project.database.LuminaDatabase
import org.example.project.shared.AppModule
import org.example.project.shared.data.AttachmentDAO
import org.example.project.shared.data.EmailDAO

class DesktopAppModule : AppModule {

    private val db by lazy {
        LuminaDatabase(
            driver = DatabaseDriverFactory().create(),
            EmailsAdapter = Emails.Adapter(
                attachments_countAdapter = IntColumnAdapter,
            ),
        )
    }

    override fun provideAccountDataSource(): AccountDataSource {
        return AccountDataSource(db)
    }

    override fun provideEmailDataSource(): EmailDataSource {
        return EmailDataSource(db)
    }

    override fun provideAttachmentDataSource(): AttachmentDataSource {
        return AttachmentDataSource(db)
    }
}