package org.example.project.mail

import com.example.Accounts
import jakarta.mail.*
import jakarta.mail.internet.ContentType
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MailDateFormat
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.angus.mail.iap.Argument
import org.eclipse.angus.mail.iap.Response
import org.eclipse.angus.mail.imap.IMAPFolder
import org.eclipse.angus.mail.imap.protocol.BODY
import org.eclipse.angus.mail.imap.protocol.FetchResponse
import org.eclipse.angus.mail.imap.protocol.IMAPProtocol
import org.eclipse.angus.mail.imap.protocol.IMAPResponse
import org.example.project.data.EmailContent
import org.example.project.data.EmailParts
import org.example.project.shared.data.AttachmentsDAO
import org.example.project.shared.data.EmailsDAO
import org.example.project.shared.utils.createCompositeKey
import org.example.project.sqldelight.AttachmentsDataSource
import org.example.project.sqldelight.EmailsDataSource
import java.io.ByteArrayInputStream
import java.util.*

class JavaMail(
    /** Index on server of first mail to fetch  */
    var start: Int,
    /** Index on server of last mail to fetch  */
    var end: Int,
    /** Emails list to update  */
    private var _emails: MutableStateFlow<MutableList<EmailsDAO>>,
    /** Attachments list to update  */
    private var _attachments: MutableStateFlow<MutableList<AttachmentsDAO>>,
    /** Emails data source  */
    private var emailsDataSource: EmailsDataSource,
    /** Attachments data source  */
    private var attachmentsDataSource: AttachmentsDataSource,
    /** Account  */
    private var account: Accounts,
    /** Messages to fetch  */
    private var messages: Array<Message>,
) : IMAPFolder.ProtocolCommand {
    //    @Throws(ProtocolException::class)
    override fun doCommand(protocol: IMAPProtocol): Any {

        val args = Argument()
        args.writeString("$start:$end")
        args.writeString("BODY[]")

        val r: Array<Response> = protocol.command("FETCH", args)
        val response: Response = r[r.size - 1]
        println("Response is responding $response")
        if (response.isOK()) {
            val props = Properties()
            props.setProperty("mail.store.protocol", "imap")
            props.setProperty("mail.mime.base64.ignoreerrors", "true")
            props.setProperty("mail.imap.partialfetch", "false")
            props.setProperty("mail.imaps.partialfetch", "false")
            val session = Session.getInstance(
                props,
            )

            var fetch: FetchResponse
            var body: BODY
            var mm: MimeMessage
            var `is`: ByteArrayInputStream? = null
            var message: Message
            var isRead = false
            var isFlagged = false

            // last response is only result summary: not contents
            for (i in 0..<r.size - 1) {

                if (r[i] is IMAPResponse) {
                    fetch = r[i] as FetchResponse
                    body = fetch.getItem(0) as BODY
                    `is` = body.getByteArrayInputStream()
                    message = messages.takeLast(50)[i]

                    try {
                        isRead = message.flags.contains(Flags.Flag.SEEN)
                        isFlagged = message.flags.contains(Flags.Flag.FLAGGED)

                        mm = MimeMessage(session, `is`)

                        val (emailBody, htmlEmailBody, attachments) = getEmailBody(mm, i)

                        val receivedDate = mm.receivedDate?.toInstant()?.toString()
                            ?: getFallbackReceivedDate(mm)?.toInstant()?.toString()
                            ?: Clock.System.now().toString() // Fallback to current time

                        val sentDate = mm.sentDate?.toInstant()?.toString()
                            ?: getFallbackReceivedDate(mm)?.toInstant()?.toString()
                            ?: Clock.System.now().toString()

                        val send = message.from
                        val internetAddress = send[0] as InternetAddress

                        _emails.value.add(
                            EmailsDAO(
                                id = null,
                                messageId = mm.messageID,
                                folderUID = null,
                                compositeKey = createCompositeKey(mm.subject, sentDate, mm.from.toString()),
                                folderName = message.folder.fullName,
                                subject = mm.subject ?: "",
                                senderAddress = internetAddress.address ?: "",
                                senderPersonal = internetAddress.personal ?: "",
                                recipients = getAllRecipients(mm).toString().toByteArray(),
                                sentDate = sentDate,
                                receivedDate = receivedDate,
                                body = emailBody,
                                htmlBody = htmlEmailBody,
                                snippet = generateSnippet(emailBody),
                                size = mm.size.toLong(),
                                isRead = isRead,
                                isFlagged = isFlagged,
                                attachmentsCount = attachments.size,
                                hasAttachments = attachments.isNotEmpty(),
                                account = account.email,
                            )
                        )

                        @Serializable
                        data class SerializableAddress(val address: String, val personal: String)

                        fun Address.toSerializableAddress(): SerializableAddress =
                            SerializableAddress(this.toString(), this.toString())

                        fun SerializableAddress.toAddress(): Address = InternetAddress(this.address, this.personal)

                        val emailId = emailsDataSource.insertEmail(
                            messageId = mm.messageID,
                            folderUID = null,
                            compositeKey = mm.subject + sentDate + mm.from.toString(),
                            folderName = message.folder.fullName,
                            subject = mm.subject,
                            address = internetAddress.address ?: "",
                            personal = internetAddress.personal ?: "",
                            recipients = getAllRecipients(mm).toString().toByteArray(),
                            sentDate = sentDate,
                            receivedDate = receivedDate,
                            body = emailBody,
                            htmlBody = htmlEmailBody,
                            snippet = generateSnippet(emailBody),
                            size = mm.size.toLong(),
                            isRead = isRead,
                            isFlagged = isFlagged,
                            attachmentsCount = attachments.size,
                            hasAttachments = attachments.isNotEmpty(),
                            account = account.email
                        )

                        for (attachment in attachments) {
                            _attachments.value.add(
                                AttachmentsDAO(
                                    id = null,
                                    emailId = emailId,
                                    fileName = attachment.fileName,
                                    mimeType = attachment.mimeType,
                                    size = attachment.size,
                                    downloadPath = "",
                                    downloaded = false
                                )
                            )
                            attachmentsDataSource.insertAttachment(
                                emailId = emailId,
                                fileName = attachment.fileName,
                                mimeType = attachment.mimeType,
                                size = attachment.size,
                            )
                        }

                        println("Email added to database")
                    } catch (e: MessagingException) {
                        print("Errored out")
                        e.printStackTrace()
                    }
                }
            }
        }
        // dispatch remaining untagged responses
        protocol.notifyResponseHandlers(r)
        protocol.handleResult(response)

        return "" + (r.size - 1)
    }

    private fun getEmailBody(mm: MimeMessage, i: Int): EmailContent {
        val attachments = mutableListOf<AttachmentsDAO>()
        val parts = EmailParts()

        when (val content = mm.content) {
            is String -> {
                // If the content is a plain string, decide which field to use based on the content type.
                if (mm.isMimeType("text/plain")) {
                    parts.plainText = content
                } else if (mm.isMimeType("text/html")) {
                    parts.htmlText = content
                } else {
                    // Fallback: assign to plain text if type is not clearly set.
                    parts.plainText = content
                }
            }

            is MimeMultipart -> {
                processMimeMultipart(content, parts, attachments)
            }

            else -> {
                parts.plainText = "Unsupported content type"
            }
        }

        return EmailContent(
            plainText = parts.plainText,
            htmlText = parts.htmlText,
            attachments = attachments
        )
    }

    private fun processMimeMultipart(
        multipart: MimeMultipart,
        parts: EmailParts,
        attachments: MutableList<AttachmentsDAO>
    ) {
        for (i in 0 until multipart.count) {
            val bodyPart = multipart.getBodyPart(i)

            // Check if this part is an attachment.
            if (Part.ATTACHMENT.equals(bodyPart.disposition, ignoreCase = true) || bodyPart.fileName != null) {
                val attachment = AttachmentsDAO(
                    id = null,
                    emailId = null,
                    fileName = bodyPart.fileName ?: "unknown",
                    mimeType = bodyPart.contentType.split(";")[0],
                    size = bodyPart.size.toLong(),
                    downloadPath = "",
                    downloaded = false
                )
                attachments.add(attachment)
            } else {
                // If the part itself is multipart, process it recursively.
                if (bodyPart.isMimeType("multipart/*")) {
                    (bodyPart.content as? MimeMultipart)?.let { nestedMultipart ->
                        processMimeMultipart(nestedMultipart, parts, attachments)
                    }
                } else {
                    // Determine the type of the content and accumulate appropriately.
                    val content = bodyPart.content

                    println("Type ${bodyPart.isMimeType("text/plain")} and ${content is String}")
                    when {
                        bodyPart.isMimeType("text/plain") && content is String -> {
                            parts.plainText += content
                        }

                        bodyPart.isMimeType("text/html") && content is String -> {
                            parts.htmlText += content
                        }
                        // Fallback: if you cannot use isMimeType(), check the header
                        content is String -> {
                            if (bodyPart.contentType.contains("text/plain", ignoreCase = true)) {
                                parts.plainText += content
                                if (i == 0) println("PLAIN $content")

                            } else if (bodyPart.contentType.contains("text/html", ignoreCase = true)) {
                                parts.htmlText += content
                                if (i == 0) println("HTML $content")
                            }
                        }
                    }
                }
            }
        }
    }


    private fun generateSnippet(emailBody: String, snippetLength: Int = 100): String {
        val plainTextBody = emailBody.replace(Regex("<[^>]*>"), "") // Remove HTML tags
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .trim()
        return if (plainTextBody.length > snippetLength) {
            plainTextBody.substring(0, snippetLength) + "..."
        } else {
            plainTextBody
        }
    }

    private fun getAllRecipients(mm: MimeMessage): List<String> {
        val toRecipients = mm.getRecipients(Message.RecipientType.TO)?.map { it.toString() } ?: emptyList()
        val ccRecipients = mm.getRecipients(Message.RecipientType.CC)?.map { it.toString() } ?: emptyList()
        val bccRecipients = mm.getRecipients(Message.RecipientType.BCC)?.map { it.toString() } ?: emptyList()
        return toRecipients + ccRecipients + bccRecipients
    }

    private fun getFallbackReceivedDate(mm: MimeMessage): Date? {
        val headers = mm.getHeader("Date")
        return if (headers != null && headers.isNotEmpty()) {
            MailDateFormat().parse(headers[0])
        } else {
            null
        }
    }
}