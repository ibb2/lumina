package org.example.project.sqldelight

import androidx.compose.runtime.LaunchedEffect
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.Emails
import com.example.project.database.LuminaDatabase
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import org.example.project.shared.data.EmailsDAO

class EmailsDataSource(db: LuminaDatabase) {

    private val queries = db.emailsTableQueries

    // Create a private MutableStateFlow to manage email updates
    private val initEmails = queries.selectAllEmails(
        mapper = { id, messageId, folderUID, compositeKey, folderName, subject, address, personal, recipients, sentDate, receivedDate, body, html_body, snippet, size, isRead, isFlagged, attachmentsCount, hasAttachments, account ->
            EmailsDAO(
                id = id,
                messageId = messageId ?: "",
                folderUID = folderUID,
                compositeKey = compositeKey,
                folderName = folderName,
                subject = subject ?: "",
                senderAddress = address ?: "",
                senderPersonal = personal ?: "",
                recipients = recipients ?: byteArrayOf(),
                sentDate = sentDate ?: "",
                receivedDate = receivedDate ?: "",
                body = body ?: "",
                htmlBody = html_body ?: "",
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
        // Fetch the latest emails and update the flow
        val updatedEmails =
            queries.selectAllEmails(mapper = { id, messageId, folderUID, compositeKey, folderName, subject, address, personal, recipients, sentDate, receivedDate, body,html_body, snippet, size, isRead, isFlagged, attachmentsCount, hasAttachments, account ->
                EmailsDAO(
                    id = id,
                    messageId = messageId ?: "",
                    folderUID = folderUID,
                    compositeKey = compositeKey,
                    folderName = folderName,
                    subject = subject ?: "",
                    senderAddress = address ?: "",
                    senderPersonal = personal ?: "", recipients = recipients ?: byteArrayOf(),
                    sentDate = sentDate ?: "",
                    receivedDate = receivedDate ?: "",
                    body = body ?: "",
                    htmlBody = html_body ?: "",
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
                _emailsFlow.emit(updatedEmails)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
  
    fun insertEmail(
        id: Long? = null,
        messageId: String,
        folderUID: Long?,
        compositeKey: String,
        folderName: String,
        subject: String,
        address: String,
        personal: String,
        recipients: ByteArray,
        sentDate: String,
        receivedDate: String,
        body: String,
        htmlBody: String,
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
                address,
                personal,
                recipients,
                sentDate,
                receivedDate,
                body,
                htmlBody,
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

    fun selectAllEmailsForAccount(emailAddress: String): List<EmailsDAO> = queries.selectAllEmailsForAccount(
        emailAddress,
        mapper = { id, messageId, folderUID, compositeKey, folderName, subject, address, personal, recipients, sentDate, receivedDate, body, html_body, snippet, size, isRead, isFlagged, attachmentsCount, hasAttachments, account ->
            EmailsDAO(
                id = id,
                messageId = messageId ?: "",
                folderUID = folderUID,
                compositeKey = compositeKey,
                folderName = folderName,
                subject = subject ?: "",
                senderAddress = address ?: "",
                senderPersonal = personal ?: "",
                recipients = recipients ?: byteArrayOf(),
                sentDate = sentDate ?: "",
                receivedDate = receivedDate ?: "",
                body = body ?: "",
                htmlBody = html_body ?: "",
                snippet = snippet ?: "",
                size = size ?: 0,
                isRead = isRead,
                isFlagged = isFlagged,
                attachmentsCount = attachmentsCount,
                hasAttachments = hasAttachments,
                account = account
            )
        }).executeAsList()


    fun updateEmailReadStatus(compositeKey: String, isRead: Boolean) =
        queries.updateEmailReadStatus(isRead, compositeKey)

    fun deleteEmail(id: Long) = queries.deleteEmail(id)

    // Search
    private val all: Flow<List<EmailsDAO>>
        get() = queries.selectAllEmails(mapper = { id, messageId, folderUID, compositeKey, folderName, subject, address, personal, recipients, sentDate, receivedDate, body, html_body, snippet, size, isRead, isFlagged, attachmentsCount, hasAttachments, account ->
            EmailsDAO(
                id = id,
                messageId = messageId ?: "",
                folderUID = folderUID,
                compositeKey = compositeKey,
                folderName = folderName,
                subject = subject ?: "",
                senderAddress = address ?: "",
                senderPersonal = personal ?: "",
                recipients = recipients ?: byteArrayOf(),
                sentDate = sentDate ?: "",
                receivedDate = receivedDate ?: "",
                body = body ?: "",
                htmlBody = html_body ?: "",
                snippet = snippet ?: "",
                size = size ?: 0,
                isRead = isRead ?: false,
                isFlagged = isFlagged ?: false,
                attachmentsCount = attachmentsCount ?: 0,
                hasAttachments = hasAttachments ?: false,
                account = account
            )
        }).asFlow().mapToList(Dispatchers.IO)

    fun search(query: String): Flow<List<EmailsDAO>> {
        return if (query.isEmpty()) {
            all
        } else {
            queries.search(
                query,
                mapper = { id, messageId, folderUID, compositeKey, folderName, subject, address, personal, recipients, sentDate, receivedDate, body, html_body, snippet, size, isRead, isFlagged, attachmentsCount, hasAttachments, account ->
                    EmailsDAO(
                        id = id,
                        messageId = messageId ?: "",
                        folderUID = folderUID,
                        compositeKey = compositeKey,
                        folderName = folderName,
                        subject = subject ?: "",
                        senderAddress = address ?: "",
                        senderPersonal = personal ?: "",
                        recipients = recipients ?: byteArrayOf(),
                        sentDate = sentDate ?: "",
                        receivedDate = receivedDate ?: "",
                        body = body ?: "",
                        htmlBody = html_body ?: "",
                        snippet = snippet ?: "",
                        size = size ?: 0,
                        isRead = isRead ?: false,
                        isFlagged = isFlagged ?: false,
                        attachmentsCount = attachmentsCount.toInt() ?: 0,
                        hasAttachments = hasAttachments ?: false,
                        account = account
                    )
                }
            ).asFlow().mapToList(Dispatchers.IO)
        }
    }

}