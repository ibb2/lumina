package org.example.project.sqldelight

import com.example.Emails
import com.example.project.database.LuminaDatabase
import org.example.project.shared.data.EmailsDAO

class EmailsDataSource(db: LuminaDatabase) {

    private val queries = db.emailsTableQueries

    fun insertEmail(
        id: Long? = null,
        messageId: String,
        folderUID: Long,
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
        isRead: Boolean,
        isFlagged: Boolean,
        attachmentsCount: Int,
        hasAttachments: Boolean,
        account: String,
    ): Long {

        return queries.transactionWithResult {
            queries.insertEmail(
                id,
                messageId,
                folderUID,
                compositeKey,
                folderName,
                subject,
                sender,
                recipients,
                sentDate,
                receivedDate,
                body,
                snippet,
                size,
                isRead,
                isFlagged,
                attachmentsCount,
                hasAttachments,
                account
            )
            queries.lastInsertedRowId().executeAsOne()
        }
    }

    fun remove() = queries.removeAllEmails()

    fun selectAllEmails(): List<EmailsDAO> = queries.selectAllEmails(
        mapper = { id, messageId, folderUID, compositeKey, folderName, subject, sender, recipients, sentDate, receivedDate, body, snippet, size, isRead, isFlagged, attachmentsCount, hasAttachments, account ->
            EmailsDAO(
                id = id,
                messageId = messageId ?: "",
                folderUID = folderUID,
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
                isRead = isRead ?: false,
                isFlagged = isFlagged ?: false,
                attachmentsCount = attachmentsCount ?: 0,
                hasAttachments = hasAttachments ?: false,
                account = account
            )
        }
    ).executeAsList()

    fun selectEmail(compositeKey: String): List<EmailsDAO> = queries.selectEmail(
        compositeKey,
        mapper = { id, messageId, folderUID, compositeKey, folderName, subject, sender, recipients, sentDate, receivedDate, body, snippet, size, isRead, isFlagged, attachmentsCount, hasAttachments, account ->
            EmailsDAO(
                id = id,
                messageId = messageId ?: "",
                folderUID = folderUID,
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
                isRead = isRead ?: false,
                isFlagged = isFlagged ?: false,
                attachmentsCount = attachmentsCount ?: 0,
                hasAttachments = hasAttachments ?: false,
                account = account
            )
        }
    ).executeAsList()

    fun updateEmailReadStatus(compositeKey: String, isRead: Boolean) = queries.updateEmailReadStatus(isRead, compositeKey)
}