package org.example.project.shared

import org.example.project.sqldelight.AccountsDataSource
import org.example.project.sqldelight.AttachmentsDataSource
import org.example.project.sqldelight.EmailsDataSource

interface AppModule {

    fun provideAccountsDataSource(): AccountsDataSource

    fun provideEmailsDataSource(): EmailsDataSource

    fun provideAttachmentsDataSource(): AttachmentsDataSource
}