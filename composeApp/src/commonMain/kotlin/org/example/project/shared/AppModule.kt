package org.example.project.shared

import org.example.project.sqldelight.AccountDataSource

interface AppModule {

    fun provideAccountDataSource(): AccountDataSource
}