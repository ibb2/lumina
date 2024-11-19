package org.example.project.sqldelight

import com.example.project.database.LuminaDatabase
import org.example.project.shared.data.Email

class EmailDataSource(db: LuminaDatabase) {

    private val queries = db.emailTableQueries

    fun insertEmail(
        id: Long,
        compositeKey: String,
        folderName: String,
        subject: String,
        sender: String,
        recipients: ByteArray,
        sentDate: String,
        receivedDate: String,
        body: String,
        snippet: String,
        size: Long,
        isRead: Int,
        isFlagged: Int,
        attachmentsCount: Int,
        hasAttachments: Int,
        account: String
    ): Unit = queries.insertEmail(id, compositeKey, folderName, subject, sender, recipients, sentDate, receivedDate, body, snippet, size, isRead, isFlagged, attachmentsCount, hasAttachments, account)

    fun remove() = queries.removeAllEmails()

    fun selectAllEmails(): List<Email> = queries.selectAllEmails(
        mapper = { id, compositeKey, folderName, subject, sender, recipients, sentDate, receivedDate, body, snippet, size, isRead, isFlagged, attachmentsCount, hasAttachments, account ->
            Email(
                id = id,
                compositeKey = compositeKey,
                folderName = folderName,
                subject = subject ?: "",
                sender = sender ?: "",
                recipients = recipients ?: byteArrayOf(),
                sentDate = sentDate ?: "",
                receivedDate = receivedDate ?: "",
                body = body ?: "",
                snippet = snippet ?: "",
                size = size ?: 0,
                isRead = isRead ?: 0,
                isFlagged = isFlagged ?: 0,
                attachmentsCount = attachmentsCount ?: 0,
                hasAttachments = hasAttachments ?: 0,
                account = account
            )
        }
    ).executeAsList()
}