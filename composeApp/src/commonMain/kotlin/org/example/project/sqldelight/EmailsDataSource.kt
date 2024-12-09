package org.example.project.sqldelight

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.Emails
import com.example.project.database.LuminaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.example.project.shared.data.EmailsDAO

class EmailsDataSource(db: LuminaDatabase) {

    private val queries = db.emailsTableQueries

    fun insertEmail(
        id: Long? = null,
        messageId: String,
        folderUID: Long?,
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
                isRead = isRead,
                isFlagged = isFlagged,
                attachmentsCount = attachmentsCount,
                hasAttachments = hasAttachments,
                account = account
            )
        }).executeAsList()

    fun selectAllEmailsForAccount(emailAddress: String): List<EmailsDAO> = queries.selectAllEmailsForAccount(
        emailAddress,
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
                isRead = isRead,
                isFlagged = isFlagged,
                attachmentsCount = attachmentsCount,
                hasAttachments = hasAttachments,
                account = account
            )
        }).executeAsList()

    fun selectAllEmailsFlow(): Flow<List<EmailsDAO>> = queries.selectAllEmails(
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
                isRead = isRead,
                isFlagged = isFlagged,
                attachmentsCount = attachmentsCount,
                hasAttachments = hasAttachments,
                account = account
            )
        }).asFlow().mapToList(context = Dispatchers.IO)


//    fun selectEmail(compositeKey: String): Flow<EmailsDAO> = queries.selectEmail(
//        compositeKey).executeAsList().asFlow().map { it ->
//            EmailsDAO(
//                id = it.id,
//                messageId = it.message_id,
//                folderUID = it.folder_uid,
//                compositeKey = it.composite_key,
//                folderName = it.folder_name,
//                subject = it.subject ?: "",
//                sender = it.sender ?: "",
//                recipients = it.recipients ?: byteArrayOf(),
//                sentDate = it.sent_date ?: "",
//                receivedDate = it.received_date ?: "",
//                body = it.body ?: "",
//                snippet = it.snippet ?: "",
//                size = it.size ?: 0,
//                isRead = it.is_read ?: false,
//                isFlagged = it.is_flagged ?: false,
//                attachmentsCount = it.attachments_count ?: 0,
//                hasAttachments = it.has_attachments ?: false,
//                account = it.account
//            )
//    }

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
        }).executeAsList()


    fun updateEmailReadStatus(compositeKey: String, isRead: Boolean) =
        queries.updateEmailReadStatus(isRead, compositeKey)

    fun deleteEmail(id: Long) = queries.deleteEmail(id)

    // Search

    private val all: Flow<List<EmailsDAO>>
        get() = queries.selectAllEmails(mapper = { id, messageId, folderUID, compositeKey, folderName, subject, sender, recipients, sentDate, receivedDate, body, snippet, size, isRead, isFlagged, attachmentsCount, hasAttachments, account ->
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
        }).asFlow().mapToList(
            context = Dispatchers.IO
        )

    fun search(query: String): Flow<List<EmailsDAO>> {
        return if (query.isEmpty()) {
            all
        } else {
            queries.search(
                query,
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
            ).asFlow().mapToList(
                context = Dispatchers.IO
            )
        }
    }

}