package org.example.project.sqldelight

import com.example.project.database.LuminaDatabase
import org.example.project.Email

class EmailDataSource(db: LuminaDatabase) {

    private val queries = db.emailTableQueries

    fun insertEmail(fromUser: String, subject: String, body: String, toUser: String?, cc: String?, bcc: String?, account: String): Unit = queries.insertEmail(
        fromUser, toUser, cc, bcc, subject, body, account)

    fun remove() = queries.removeAllEmails()


    fun selectAllEmails(): List<Email> = queries.selectAllEmails(
        mapper = { id, fromUser, toUser, cc, bcc, subject, body, account -> Email( from = fromUser, subject = subject, body = body, to = toUser, cc = cc, bcc= bcc, account = account) }
    ).executeAsList()
}