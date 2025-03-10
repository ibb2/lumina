package org.example.project.sqldelight

import androidx.compose.runtime.LaunchedEffect
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.Emails
import com.example.project.database.LuminaDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import org.example.project.shared.data.EmailsDAO

class EmailsDataSource(db: LuminaDatabase) {

    private val queries = db.emailsTableQueries

    // Create a private MutableStateFlow to manage email updates
    private val initEmails = queries.selectAllEmails(
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
        }
    ).executeAsList()
    private val _emailsFlow = MutableStateFlow<List<EmailsDAO>>(initEmails)

    // Public immutable flow for observing
    val emailsFlow: StateFlow<List<EmailsDAO>> = _emailsFlow.asStateFlow()

    // Method to trigger updates
    fun notifyEmailsUpdated() {
        println("Emitting")
        // Fetch the latest emails and update the flow
        val updatedEmails = queries.selectAllEmails(mapper = { id, messageId, folderUID, compositeKey, folderName, subject, sender, recipients, sentDate, receivedDate, body, snippet, size, isRead, isFlagged, attachmentsCount, hasAttachments, account ->
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

        CoroutineScope(Dispatchers.Default).launch {
            try {
                println("Emitting: Attempting to emit emails")
                _emailsFlow.emit(updatedEmails)
                println("Emitting: Successfully emitted emails")
            } catch (e: Exception) {
                println("Emitting: Error during emission - ${e.message}")
                e.printStackTrace()
            }
        }

        println("Emails updated: ${updatedEmails.size}")
    }

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
            val insertedId = queries.lastInsertedRowId().executeAsOne()
            // Notify that emails have been updated
            notifyEmailsUpdated()

            insertedId
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

    // Alternative flow method using the StateFlow
    fun selectAllFlow(): StateFlow<List<EmailsDAO>> = emailsFlow


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
        }).asFlow().mapToList(context = Dispatchers.IO).distinctUntilChanged().flowOn(Dispatchers.Main)


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

    private val all: List<EmailsDAO>
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
        }).executeAsList()

    fun search(query: String): List<EmailsDAO> {
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
                        attachmentsCount = attachmentsCount.toInt() ?: 0,
                        hasAttachments = hasAttachments ?: false,
                        account = account
                    )
                }
            ).executeAsList()
        }
    }

}