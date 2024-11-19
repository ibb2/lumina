package org.example.project.shared

import org.example.project.sqldelight.AccountDataSource
import org.example.project.sqldelight.AttachmentDataSource
import org.example.project.sqldelight.EmailDataSource

interface AppModule {

    fun provideAccountDataSource(): AccountDataSource

    fun provideEmailDataSource(): EmailDataSource

    fun provideAttachmentDataSource(): AttachmentDataSource
}