package org.example.project.sqldelight

import com.example.project.database.LuminaDatabase

class AccountDataSource(db: LuminaDatabase) {

    private val queries = db.accountTableQueries

    fun insert(emailAddress: String) = queries.insertAccount(emailAddress)

    fun remove(emailAddress: String) = queries.removeAccount(emailAddress)

    fun removeAll() = queries.removeAllAccounts()

}