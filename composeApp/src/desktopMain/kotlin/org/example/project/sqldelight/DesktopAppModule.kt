package org.example.project.sqldelight

import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import com.example.Attachment
import com.example.Email
import com.example.project.database.LuminaDatabase
import org.example.project.shared.AppModule

class DesktopAppModule : AppModule {

    private val db by lazy {
        LuminaDatabase(
            driver = DatabaseDriverFactory().create(),
            AttachmentAdapter = Attachment.Adapter(IntColumnAdapter),
            EmailAdapter = Email.Adapter(
                is_readAdapter = IntColumnAdapter,
                is_flaggedAdapter = IntColumnAdapter,
                attachments_countAdapter = IntColumnAdapter,
                has_attachmentsAdapter = IntColumnAdapter,

            )
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