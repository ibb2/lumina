package org.example.project.sqldelight

import com.example.Email
import com.example.project.database.LuminaDatabase

class EmailDataSource(db: LuminaDatabase) {

    private val queries = db.emailTableQueries

    fun insertEmail(fromUser: String, subject: String, body: String, toUser: String?, cc: String?, bcc: String?, account: String) = queries.insertEmail(fromUser, toUser, cc, bcc, subject, body, account)

    fun remove() = queries.removeAllEmails()


    fun selectAll(): List<Email> = queries.selectAllEmails().executeAsList()
}