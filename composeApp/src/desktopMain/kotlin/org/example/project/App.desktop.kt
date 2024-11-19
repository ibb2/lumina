package org.example.project

import com.example.AccountsTableQueries
import com.example.EmailsTableQueries
import jakarta.mail.*
import jakarta.mail.internet.MimeMultipart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.angus.mail.imap.IMAPFolder
import org.example.project.mail.JavaMail
import org.example.project.shared.data.EmailsDAO
import org.example.project.sqldelight.EmailsDataSource
import java.util.*
import kotlin.properties.Delegates


actual class EmailService {

    private val emails = mutableListOf<EmailsDAO>()
    private var totalEmailCount = 0
    private val _emailsRead = MutableStateFlow(0)
    actual val emailsRead: StateFlow<Int> = _emailsRead
    actual var emailCount by Delegates.observable(0) { property, oldValue, newValue ->
        println("Value changed ${property.name} from $oldValue to $newValue")
        _emailsRead.value = newValue
        println("Emails read: $emailsRead")
    }


    actual suspend fun getEmails(
        emailDataSource: EmailsDataSource,
        emailTableQueries: EmailsTableQueries,
        accountQueries: AccountsTableQueries,
        emailAddress: String,
        password: String
    ): List<EmailsDAO> {

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
//        val emailsExist = doEmailsExist(emailTableQueries, emailDataSource)

//        if (emailsExist) {
//            val emails = returnEmails(emailTableQueries, emailDataSource)
//            return emails
//        }

//        fetchEmailBodies(emailAddress, emailTableQueries, emailDataSource, accountQueries, store)

        val inbox = store.getFolder("INBOX").apply { open(Folder.READ_ONLY) } as IMAPFolder
        val messages = inbox.getMessages()
        val account = accountQueries.selectAccount(emailAddress = emailAddress).executeAsList()

        efficientGetContents(
            emailAddress,
            emailTableQueries,
            emailDataSource,
            accountQueries,
            store,
            emails,
            account,
            inbox,
            inbox,
            messages,
            properties
        )

        return emails
    }

    fun doEmailsExist(emailTableQueries: EmailsTableQueries, emailDataSource: EmailsDataSource): Boolean {
        val emailsExist = emailTableQueries.selectAllEmails().executeAsList()

        return emailsExist.isNotEmpty()
    }

    fun fetchEmailBodies(
        emailAddress: String,
        emailTableQueries: EmailsTableQueries,
        emailDataSource: EmailsDataSource,
        accountQueries: AccountsTableQueries,
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

        val fp = FetchProfile()
        fp.add(FetchProfile.Item.ENVELOPE)
        fp.add(IMAPFolder.FetchProfileItem.FLAGS)
        fp.add(IMAPFolder.FetchProfileItem.CONTENT_INFO)

        val folder = store.getFolder("INBOX").apply { open(Folder.READ_ONLY) }
        val msgs = folder.messages

        val messages: List<Message> = folder.messages.takeLast(50)
//        folder.fetch(msgs, fp)
        // Email UIDs
        val uf: UIDFolder = folder as UIDFolder

        // Account
        val account = accountQueries.selectAccount(emailAddress = emailAddress).executeAsList()

//        totalEmailCount = 50

//        for (message in messages) {
//            val emailUID = uf.getUID(message)
//            println("Message: ${message.from}")
//            emails.add(
//                Email(
//                    id = emailUID,
//                    from = message.from?.joinToString(),
//                    subject = message.subject ?: "",
//                    body = getEmailBody(message),
//                    to = "",
//                    cc = null,
//                    bcc = null,
//                    account = account[0]
//                )
//            )
//
////            emailTableQueries.insertEmail(
////                id = emailUID,
////                from_user = message.from?.joinToString() ?: "",
////                subject = message.subject ?: "",
////                body = getEmailBody(message),
////                to_user = "",
////                cc = null,
////                bcc = null,
////                account = account[0]
////            )
//
//            emailCount++
//        }

//        for (message in folder.getMessages().takeLast(50)) {
//            val emailUID = uf.getUID(message)
//            println("Message: ${message.from}")
//            emails.add(
//                Email(
//                    id = emailUID,
//                    from = message.from?.joinToString(),
//                    subject = message.subject ?: "",
//                    body = getEmailBody(message),
//                    to = "",
//                    cc = null,
//                    bcc = null,
//                    account = account[0]
//                )
//            )

//            emailTableQueries.insertEmail(
//                id = emailUID,
//                from_user = message.from?.joinToString() ?: "",
//                subject = message.subject ?: "",
//                body = getEmailBody(message),
//                to_user = "",
//                cc = null,
//                bcc = null,
//                account = account[0]
//            )
//
//            emailCount++
//        }

        folder.close(false)
        store.close()
    }

    @Throws(MessagingException::class)
    fun efficientGetContents(
        emailAddress: String,
        emailTableQueries: EmailsTableQueries,
        emailDataSource: EmailsDataSource,
        accountQueries: AccountsTableQueries,
        store: Store,
        emails: MutableList<EmailsDAO>,
        account: List<String>,
        folder: Folder,
        inbox: IMAPFolder,
        messages: Array<Message>,
        properties: Properties
    ): Int {
        val fp = FetchProfile()
        fp.add(FetchProfile.Item.FLAGS)
        fp.add(FetchProfile.Item.ENVELOPE)
        inbox.fetch(messages, fp)
        var index = 0
        val nbMessages = inbox.getMessages().takeLast(50).size
        val maxDoc = 5000
        val maxSize: Long = 100000000 // 100Mo
        totalEmailCount = nbMessages

        // Email UIDs
        val uf: UIDFolder = inbox

        // Message numbers limit to fetch
        var start: Int
        var end: Int

        while (index < nbMessages) {
            start = messages[index].messageNumber
            var docs = 0
            var totalSize = 0
            var noskip = true // There are no jumps in the message numbers
            // list
            var notend = true
            // Until we reach one of the limits
            while (docs < maxDoc && totalSize < maxSize && noskip && notend) {
                docs++
                totalSize += messages[index].size
                index++
                if ((index < nbMessages).also { notend = it }) {
                    noskip = (messages[index - 1].messageNumber + 1 === messages[index]
                        .messageNumber)
                }
            }

            end = messages[index - 1].messageNumber
            inbox.doCommand(JavaMail(start, end, emails, uf, properties, account, inbox.getMessages().takeLast(50).toTypedArray()))

            println("Fetching contents for $start:$end")
            println(
                ("Size fetched = " + (totalSize / 1000000)
                        + " Mo")
            )
        }

        return nbMessages
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

    actual suspend fun deleteEmails(emailDataSource: EmailsDataSource) {
        emailDataSource.remove()
    }

    actual fun getEmailCount(emailDataSource: EmailsDataSource): Int {
//        totalEmailCount = emailDataSource.selectAllEmails().size
        return totalEmailCount
    }


    actual fun returnEmails(
        emailTableQueries: EmailsTableQueries,
        emailDataSource: EmailsDataSource
    ): List<EmailsDAO> {
        TODO("Not yet implemented")
    }

}

