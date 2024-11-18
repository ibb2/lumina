package org.example.project

import com.example.Account
import com.example.AccountTableQueries
import com.example.EmailTableQueries
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.Store
import jakarta.mail.UIDFolder
import jakarta.mail.internet.MimeMultipart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.example.project.sqldelight.DesktopAppModule
import org.example.project.sqldelight.EmailDataSource
import org.koin.core.component.KoinComponent
import java.util.*
import kotlin.properties.Delegates


actual class EmailService {

    private val emails = mutableListOf<Email>()
    private var totalEmailCount = 0
    private val _emailsRead = MutableStateFlow(0)
    actual val emailsRead: StateFlow<Int> = _emailsRead
    actual var emailCount by Delegates.observable(0) { property, oldValue, newValue ->
        println("Value changed ${property.name} from $oldValue to $newValue")
        _emailsRead.value = newValue
        println("Emails read: $emailsRead")
    }


    actual suspend fun getEmails(
        emailDataSource: EmailDataSource,
        emailTableQueries: EmailTableQueries,
        accountQueries: AccountTableQueries,
        emailAddress: String,
        password: String
    ): List<Email> {

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
        val store: Store = session.getStore("imap").apply {
            connect(
                properties.getProperty("mail.imap.host"),
                properties.getProperty("mail.imap.username"),
                properties.getProperty("mail.imap.password")
            )
        }

        // Check if emails exist in db
        val emailsExist = doEmailsExist(emailTableQueries, emailDataSource)

        if (emailsExist) {
            val emails = returnEmails(emailTableQueries, emailDataSource)
            return emails
        }

        fetchEmailBodies(emailAddress, emailTableQueries, emailDataSource, accountQueries, store)

        return emails
    }

    fun doEmailsExist(emailTableQueries: EmailTableQueries, emailDataSource: EmailDataSource): Boolean {
        val emailsExist = emailTableQueries.selectAllEmails().executeAsList()

        return emailsExist.isNotEmpty()
    }

    fun fetchEmailBodies(
        emailAddress: String,
        emailTableQueries: EmailTableQueries,
        emailDataSource: EmailDataSource,
        accountQueries: AccountTableQueries,
        store: Store
    ) {
        /**
         * Fetches the bodies of the emails in the INBOX folder.
         *
         * This uses the JavaMail API to connect to the IMAP server and fetch the emails.
         * It then uses the sqldelight library to insert the emails into the sqlite database.
         * This should only be run on initial setup of the app.
         *
         * @param emailAddress The email address to use for the imap connection.
         * @param emailTableQueries The sqldelight query object for the email table.
         * @param emailDataSource The sqldelight data source for the email table.
         * @param accountQueries The sqldelight query object for the account table.
         * @param store The JavaMail store object.
         */

        val folder = store.getFolder("INBOX").apply { open(Folder.READ_ONLY) }
        val messages: List<Message> = folder.messages.takeLast(10)

        // Email UIDs
        val uf: UIDFolder = folder as UIDFolder

        // Account
        val account = accountQueries.selectAccount(emailAddress = emailAddress).executeAsList()


        for ( message in messages) {
            val emailUID = uf.getUID(message)
            emails.add(
                Email(
                    id = emailUID,
                    from = message.from?.joinToString(),
                    subject = message.subject ?: "",
                    body = getEmailBody(message),
                    to = "",
                    cc = null,
                    bcc = null,
                    account = account[0]
                )
            )

            emailTableQueries.insertEmail(
                id = emailUID,
                from_user = message.from?.joinToString() ?: "",
                subject = message.subject ?: "",
                body = getEmailBody(message),
                to_user = "",
                cc = null,
                bcc = null,
                account = account[0]
            )

            emailCount++
        }

        folder.close(false)
        store.close()
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

    actual fun returnEmails(emailTableQueries: EmailTableQueries, emailDataSource: EmailDataSource): List<Email> {
        val listOfEmails = emailTableQueries.selectAllEmails().executeAsList()
        listOfEmails.forEach { email ->
            emails.add(
                Email(
                    id = email.id,
                    from = email.from_user,
                    subject = email.subject,
                    body = email.body,
                    to = email.to_user,
                    cc = email.cc,
                    bcc = email.bcc,
                    account = email.account
                )
            )
        }
        return emails
    }

    actual suspend fun deleteEmails(emailDataSource: EmailDataSource) {
        emailDataSource.remove()
    }

    actual fun getEmailCount(emailDataSource: EmailDataSource): Int {
        totalEmailCount = emailDataSource.selectAllEmails().size
        return totalEmailCount
    }

}

