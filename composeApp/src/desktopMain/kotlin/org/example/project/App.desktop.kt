package org.example.project

import com.example.Account
import com.example.AccountTableQueries
import com.example.EmailTableQueries
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.Store
import jakarta.mail.internet.MimeMultipart
import org.example.project.sqldelight.DesktopAppModule
import org.example.project.sqldelight.EmailDataSource
import org.koin.core.component.KoinComponent
import java.util.*



actual class EmailService {

    private val emails = mutableListOf<Email>()
    private var emailCount = 0

    actual suspend  fun getEmails(emailDataSource: EmailDataSource, emailTableQueries: EmailTableQueries, accountQueries: AccountTableQueries, emailAddress: String, password: String): List<Email> {

        val properties: Properties = Properties().apply {
            put("mail.imap.host", "imap.gmail.com")
            put("mail.imap.username", emailAddress)
            put("mail.imap.password", password)
            put("mail.imap.port", "993")
            put("mail.imap.ssl.enable", "true")
            put("mail.imap.connectiontimeout", 10000)
            put("mail.imap.timeout", 10000)
            put("mail.imap.partialfetch", "false");
            put("mail.imap.fetchsize", "1048576");

        }

        val session = Session.getInstance(properties)
        val store: Store = session.getStore("imap").apply { connect(properties.getProperty("mail.imap.host"),properties.getProperty("mail.imap.username"), properties.getProperty("mail.imap.password")) }

        // Check if emails exist in db
        val emailsExist = doEmailsExist(emailTableQueries, emailDataSource)

//        if (emailsExist) {
//            val emails = returnEmails(emailTableQueries, emailDataSource)
//            return emails
//        }

        val email = fetchEmailBodies(emailAddress,emailTableQueries,emailDataSource, accountQueries, store)

        emails.addAll(email)

        return emails
    }

    fun doEmailsExist(emailTableQueries: EmailTableQueries, emailDataSource: EmailDataSource): Boolean {
        val emailsExist = emailTableQueries.selectAllEmails().executeAsList()

        return emailsExist.isNotEmpty()
    }

    fun returnEmails(emailTableQueries: EmailTableQueries, emailDataSource: EmailDataSource): List<Email> {
        val emails = emailDataSource.selectAllEmails()
        return emails
    }

    fun fetchEmailBodies(emailAddress: String, emailTableQueries: EmailTableQueries,emailDataSource: EmailDataSource, accountQueries: AccountTableQueries, store: Store): List<Email> {
        val folder = store.getFolder("INBOX").apply { open(Folder.READ_ONLY) }
        println("Number of messages: ${folder.messageCount}")
        val messages: List<Message> = folder.messages.takeLast(10)

        // Account
        val account = accountQueries.selectAccount(emailAddress = emailAddress).executeAsList()


        for ((index, message) in messages.withIndex()) {
            println(message.subject)
            emails.add(Email(
               from = message.from?.joinToString(), subject = message.subject?: "", body = getEmailBody(message), to = "", cc = null, bcc = null, account = account[0]
            ))
//            emails += (Email(
//               from = message.from?.joinToString(), subject = message.subject?: "", body = getEmailBody(message), to = "", cc = null, bcc = null, account = account[0]
//            ))

//            emailTableQueries.insertEmail(
//                from_user = message.from?.joinToString() ?: "",
//                subject = message.subject ?: "",
//                body = getEmailBody(message),
//                to_user = "",
//                cc = null,
//                bcc = null,
//                account = account[0]
//            )
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

    actual suspend fun deleteEmails(emailDataSource: EmailDataSource) {
        emailDataSource.remove()
    }

    actual fun getEmailCount(emailDataSource: EmailDataSource) : Int {
        emailCount = emailDataSource.selectAllEmails().size
        return emailCount
    }

}

