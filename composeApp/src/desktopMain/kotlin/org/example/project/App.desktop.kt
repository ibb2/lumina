package org.example.project

import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.Store
import jakarta.mail.internet.MimeMultipart
import java.util.*


actual class EmailService {
    actual fun getEmails(emailAddress: String, password: String): Array<Email> {

        val properties: Properties = Properties().apply {
            put("mail.store.protocol", "imap")
//            put("mail.imap.ssl.trust", "imap.gmail.com")
            put("mail.imap.username", emailAddress)
            put("mail.imap.password", password)
            put("mail.imap.host", "imap.gmail.com")
            put("mail.imap.port", "993")
            put("mail.imap.ssl.enable", "true")
            put("mail.imap.connectiontimeout", 10000)
            put("mail.imap.timeout", 10000)

        }

        val session = Session.getInstance(properties)
        val store: Store = session.getStore("imap").apply { connect(properties.getProperty("mail.imap.host"),properties.getProperty("mail.imap.username"), properties.getProperty("mail.imap.password")) }

        val email: Array<Email> = fetchEmailBodies(store)

        return email
    }

    fun fetchEmailBodies(store: Store): Array<Email> {
        val folder = store.getFolder("INBOX").apply { open(Folder.READ_ONLY) }
        val messages: List<Message> = folder.messages.slice(0..10)

        var emails: Array<Email> = emptyArray()

        for (message in messages) {
            emails += (Email(
               from = message.from?.joinToString(), subject = message.subject?: "", body = getEmailBody(message)
            ))
        }

        folder.close(false)
        store.close()

        return emails
    }

    fun getEmailBody(message: Message): String {
        return when (val content = message.content) {
            is String -> content // Plain text or HTML content
            is MimeMultipart -> getTextFromMimeMultipart(content) // Multipart email (e.g., with attachments)
            else -> "Unsupported content type"
        }
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

}