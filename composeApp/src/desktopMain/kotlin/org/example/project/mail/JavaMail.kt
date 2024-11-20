package org.example.project.mail

import jakarta.mail.*
import jakarta.mail.internet.MailDateFormat
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.format.DateTimeFormat
import org.eclipse.angus.mail.iap.Argument
import org.eclipse.angus.mail.iap.Response
import org.eclipse.angus.mail.imap.IMAPFolder
import org.eclipse.angus.mail.imap.protocol.BODY
import org.eclipse.angus.mail.imap.protocol.FetchResponse
import org.eclipse.angus.mail.imap.protocol.IMAPProtocol
import org.eclipse.angus.mail.imap.protocol.IMAPResponse
import org.example.project.shared.data.AttachmentsDAO
import org.example.project.shared.data.EmailsDAO
import org.example.project.sqldelight.AttachmentsDataSource
import org.example.project.sqldelight.EmailsDataSource
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.*

class JavaMail(
    /** Index on server of first mail to fetch  */
    var start: Int,
    /** Index on server of last mail to fetch  */
    var end: Int,
    var emails: MutableList<EmailsDAO>,
    var account: List<String>,
    var messages: Array<Message>,
    var attachmentsArray: MutableList<AttachmentsDAO>,
    var emailsDataSource: EmailsDataSource,
    var attachmentsDataSource: AttachmentsDataSource,
) : IMAPFolder.ProtocolCommand {
    //    @Throws(ProtocolException::class)
    override fun doCommand(protocol: IMAPProtocol): Any {

        val args = Argument()
        args.writeString("$start:$end")
        args.writeString("BODY[]")

        val r: Array<Response> = protocol.command("FETCH", args)
        val response: Response = r[r.size - 1]
        print("Response is responding $response")
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

            // last response is only result summary: not contents
            for (i in 0..<r.size - 1) {
                if (r[i] is IMAPResponse) {
                    fetch = r[i] as FetchResponse
                    body = fetch.getItem(0) as BODY
                    `is` = body.getByteArrayInputStream()
                    message = messages[i]
                    try {
                        mm = MimeMessage(session, `is`)

                        val (emailBody, attachments) = getEmailBody(mm, i)

                        val receivedDate = mm.receivedDate?.toInstant()?.toString()
                            ?: getFallbackReceivedDate(mm)?.toInstant()?.toString()
                            ?: Clock.System.now().toString() // Fallback to current time

                        val sentDate = mm.sentDate?.toInstant()?.toString()
                            ?: getFallbackReceivedDate(mm)?.toInstant()?.toString()
                            ?: Clock.System.now().toString()

                        emails.add(
                            EmailsDAO(
                                id = null,
                                compositeKey = mm.subject + sentDate + mm.from.toString(),
                                folderName = message.folder.fullName,
                                subject = mm.subject,
                                sender = mm.from[0].toString(),
                                recipients = getAllRecipients(mm).toString().toByteArray(),
                                sentDate = sentDate,
                                receivedDate = receivedDate,
                                body = emailBody,
                                snippet = generateSnippet(emailBody),
                                size = mm.size.toLong(),
                                isRead = message.isSet(Flags.Flag.SEEN),
                                isFlagged = message.isSet(Flags.Flag.FLAGGED),
                                attachmentsCount = attachments.size,
                                hasAttachments = attachments.isNotEmpty(),
                                account = account[0],
                            )
                        )

                        emailsDataSource.insertEmail(
                            compositeKey = mm.subject + sentDate + mm.from.toString(),
                            folderName = message.folder.fullName,
                            subject = mm.subject,
                            sender = mm.from[0].toString(),
                            recipients = getAllRecipients(mm).toString().toByteArray(),
                            sentDate = sentDate,
                            receivedDate = receivedDate,
                            body = emailBody,
                            snippet = generateSnippet(emailBody),
                            size = mm.size.toLong(),
                            isRead = message.isSet(Flags.Flag.SEEN),
                            isFlagged = message.isSet(Flags.Flag.FLAGGED),
                            attachmentsCount = attachments.size,
                            hasAttachments = attachments.isNotEmpty(),
                            account = account[0],
                        )

                        for (attachment in attachments) {
                            attachmentsArray.add(
                                AttachmentsDAO(
                                    id = null,
                                    emailId = emailsDataSource.lastInsertedRowId(),
                                    fileName = attachment.fileName,
                                    mimeType = attachment.mimeType,
                                    size = attachment.size,
                                    downloadPath = "",
                                    downloaded = false
                                )
                            )
                            println("${i} : ${emailsDataSource.lastInsertedRowId()}")
//                            attachmentsDataSource.insertAttachment(
//                                emailId = emailsDataSource.lastInsertedRowId(),
//                                fileName = attachment.fileName,
//                                mimeType = attachment.mimeType,
//                                size = attachment.size,
//                            )
                        }
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

    fun getEmailBody(mm: MimeMessage, i: Int): Pair<String, List<AttachmentsDAO>> {
        val attachments = mutableListOf<AttachmentsDAO>()
        val emailBody = when (val content = mm.content) {
            is String -> content // Plain text or HTML content
            is MimeMultipart -> processMimeMultipart(content, attachments) // Multipart email (e.g., with attachments)
            else -> "Unsupported content type"

        }

        return Pair(emailBody, attachments)
    }

    fun getTextFromMimeMultipart(mimeMultipart: MimeMultipart): String {
        val result = StringBuilder()
        for (i in 0 until mimeMultipart.count) {
            val bodyPart = mimeMultipart.getBodyPart(i)

            when {
                bodyPart.isMimeType("text/plain") -> result.append(bodyPart.content)
                bodyPart.isMimeType("text/html") -> result.append(bodyPart.content) // Optionally, ignore or prefer text/plain
                bodyPart.content is MimeMultipart -> result.append(
                    getTextFromMimeMultipart(bodyPart.content as MimeMultipart)
                )
            }
        }
        return result.toString()
    }

    fun processMimeMultipart(multipart: MimeMultipart, attachments: MutableList<AttachmentsDAO>): String {
        var body = ""
        for (i in 0 until multipart.count) {
            val bodyPart = multipart.getBodyPart(i)
            if (Part.ATTACHMENT.equals(bodyPart.disposition, ignoreCase = true) || bodyPart.fileName != null) {
                // This is an attachment
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
                // This is the body
                body += when (val content = bodyPart.content) {
                    is String -> content
                    is MimeMultipart -> processMimeMultipart(content, attachments)
                    else -> ""
                }
            }
        }
        return body
    }


    fun generateSnippet(emailBody: String, snippetLength: Int = 100): String {
        val plainTextBody = emailBody.replace(Regex("<[^>]*>"), "") // Remove HTML tags
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .trim()
        return if (plainTextBody.length > snippetLength) {
            plainTextBody.substring(0, snippetLength) + "..."
        } else {
            plainTextBody
        }
    }

    fun getAllRecipients(mm: MimeMessage): List<String> {
        val toRecipients = mm.getRecipients(Message.RecipientType.TO)?.map { it.toString() } ?: emptyList()
        val ccRecipients = mm.getRecipients(Message.RecipientType.CC)?.map { it.toString() } ?: emptyList()
        val bccRecipients = mm.getRecipients(Message.RecipientType.BCC)?.map { it.toString() } ?: emptyList()
        return toRecipients + ccRecipients + bccRecipients
    }

    fun getFallbackReceivedDate(mm: MimeMessage): Date? {
        val headers = mm.getHeader("Date")
        return if (headers != null && headers.isNotEmpty()) {
            MailDateFormat().parse(headers[0])
        } else {
            null
        }
    }
}