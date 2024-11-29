package org.example.project

import androidx.compose.runtime.*
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import com.example.Accounts
import com.example.AccountsTableQueries
import com.example.Emails
import com.example.EmailsTableQueries
import com.example.project.database.LuminaDatabase
import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.search.MessageIDTerm
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.eclipse.angus.mail.imap.IMAPFolder
import org.example.project.data.NewEmail
import org.example.project.mail.JavaMail
import org.example.project.networking.*
import org.example.project.shared.data.AccountsDAO
import org.example.project.shared.data.AttachmentsDAO
import org.example.project.shared.data.EmailsDAO
import org.example.project.sqldelight.AccountsDataSource
import org.example.project.sqldelight.AttachmentsDataSource
import org.example.project.sqldelight.DatabaseDriverFactory
import org.example.project.sqldelight.EmailsDataSource
import org.example.project.utils.NetworkError
import org.example.project.utils.onError
import org.example.project.utils.onSuccess
import org.example.project.windowsNative.CredentialManager
import java.net.URI
import java.util.*
import kotlin.properties.Delegates


actual class EmailService() {

    // Database
    private lateinit var db: LuminaDatabase
    private lateinit var emailDataSource: EmailsDataSource
    private lateinit var attachmentsDataSource: AttachmentsDataSource
    private lateinit var accountsDataSource: AccountsDataSource

    private var emails = mutableListOf<EmailsDAO>()
    private val totalEmailCount = MutableStateFlow(0)
    actual val totalEmails: StateFlow<Int> = totalEmailCount
    private val _emailsRead = MutableStateFlow(0)
    actual val emailsRead: StateFlow<Int> = _emailsRead
    actual var emailCount by Delegates.observable(0) { property, oldValue, newValue ->
        println("Value changed ${property.name} from $oldValue to $newValue")
        _emailsRead.value = newValue
        println("Emails read: $emailsRead")
    }
    private val attachments: MutableList<AttachmentsDAO> = mutableListOf()
    private var totalAttachments = 0

    init {
        val driver = DatabaseDriverFactory().create()
        db = LuminaDatabase(
            driver,
            EmailsAdapter = Emails.Adapter(
                attachments_countAdapter = IntColumnAdapter
            ),
        )

        // Initialising the data sources
        emailDataSource = EmailsDataSource(db)
        attachmentsDataSource = AttachmentsDataSource(db)
        accountsDataSource = AccountsDataSource(db)

        // Setting counts for emails and attachments
        totalEmailCount.value = emailDataSource.selectAllEmails().size
        totalAttachments = attachmentsDataSource.selectAllAttachments().size
    }

    actual suspend fun getEmails(
        emailDataSource: EmailsDataSource,
        emailTableQueries: EmailsTableQueries,
        accountQueries: AccountsTableQueries,
        emailAddress: String,
        client: FirebaseAuthClient
    ): Pair<MutableList<EmailsDAO>, MutableList<AttachmentsDAO>> {

        val properties: Properties = Properties().apply {
            put("mail.imap.host", "imap.gmail.com")
            put("mail.imap.auth.mechanisms", "XOAUTH2");
            put("mail.imap.port", "993")
            put("mail.imap.ssl.enable", "true")
            put("mail.imap.connectiontimeout", 10000)
            put("mail.imap.timeout", 10000)
            put("mail.imap.partialfetch", "false");
            put("mail.imap.fetchsize", "1048576");

        }

        val session = Session.getInstance(properties)
        println("Connecting...")

        var store: Store

        try {
            val atCred = CredentialManager(emailAddress, "accessToken").returnCredentials()
            val rtCred = CredentialManager(emailAddress, "refreshToken").returnCredentials()
            val idCred = CredentialManager(emailAddress, "idToken").returnCredentials()


            store = session.getStore("imap").apply {
                connect(
                    properties.getProperty("mail.imap.host"),
                    emailAddress,
                    String(atCred?.password!!)
                )
            }
        }
        catch (e: Exception) {

            var results: TokenResponse? = null
            var errors: NetworkError? = null

            val rtCred = CredentialManager(emailAddress, "refreshToken").returnCredentials()
            val refreshTokenPassword = rtCred?.password?.let { String(it) }

            client.refreshAccessToken(refreshToken = refreshTokenPassword!!).onSuccess {
                results = it
            }.onError {
                errors = it
            }

            if (results == null) {
                throw Exception(errors?.name)
            }

            CredentialManager(emailAddress, "accessToken").registerUser(
                emailAddress,
                results!!.accessToken
            )
            CredentialManager(emailAddress, "idToken").registerUser(emailAddress, results!!.idToken!!)


            store = session.getStore("imap").apply {
                connect(
                    properties.getProperty("mail.imap.host"),
                    emailAddress,
                    results!!.accessToken
                )
            }
        }
        println("Connected")

        // Check if emails exist in db
        println("Checking for emails and attachments...")
        doEmailsExist(emailTableQueries, emailDataSource)

        val emailsExist = doEmailsExist(emailTableQueries, emailDataSource)
        val attachmentsExist = doAttachmentsExist(attachmentsDataSource)

        var attach: MutableList<AttachmentsDAO> = mutableListOf()


        if (attachmentsExist) {
            attach = returnAttachments(attachmentsDataSource)
        }

        if (emailsExist) {
            emails = returnEmails(emailTableQueries, emailDataSource)

            println("Found em")
            return Pair(emails, attach)
        }

        println("Did not find any.")
//        fetchEmailBodies(email, emailTableQueries, emailDataSource, accountQueries, store)

        println("Settings up inbox...")
        val inbox = store.getFolder("INBOX").apply { open(Folder.READ_ONLY) } as IMAPFolder
        val messages = inbox.getMessages()
        val account = accountQueries.selectAccount(email = emailAddress).executeAsList()

        println("Manually getting emails now")
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

        inbox.close(false)

        println("Got them")
        return Pair(emails, attachments)
    }

    fun doEmailsExist(emailTableQueries: EmailsTableQueries, emailDataSource: EmailsDataSource): Boolean {
        val emailsExist = emailDataSource.selectAllEmails()
        totalEmailCount.value = emailsExist.size
        return emailsExist.isNotEmpty()
    }

//    fun fetchEmailBodies(
//        emailAddress: String,
//        emailTableQueries: EmailsTableQueries,
//        emailDataSource: EmailsDataSource,
//        accountQueries: AccountsTableQueries,
//        store: Store
//    ) {
//        /**
//         * Fetches the bodies of the emails in the INBOX folder.
//         *
//         * This uses the JavaMail API to connect to the IMAP server and fetch the emails.
//         * It then uses the sqldelight library to insert the emails into the sqlite database.
//         * This should only be run on initial setup of the app.
//         *
//         * @param emailAddress The email address to use for the imap connection.
//         * @param emailTableQueries The sqldelight query object for the email table.
//         * @param emailDataSource The sqldelight data source for the email table.
//         * @param accountQueries The sqldelight query object for the account table.
//         * @param store The JavaMail store object.
//         */
//
//        val fp = FetchProfile()
//        fp.add(FetchProfile.Item.ENVELOPE)
//        fp.add(IMAPFolder.FetchProfileItem.FLAGS)
//        fp.add(IMAPFolder.FetchProfileItem.CONTENT_INFO)
//
//        val folder = store.getFolder("INBOX").apply { open(Folder.READ_ONLY) }
//        val msgs = folder.messages
//
//        val messages: List<Message> = folder.messages.takeLast(50)
////        folder.fetch(msgs, fp)
//        // Email UIDs
//        val uf: UIDFolder = folder as UIDFolder
//
//        // Account
//        val account = accountQueries.selectAccount(emailAddress = emailAddress).executeAsList()
//
////        totalEmailCount = 50
//
////        for (message in messages) {
////            val emailUID = uf.getUID(message)
////            println("Message: ${message.from}")
////            emails.add(
////                Email(
////                    id = emailUID,
////                    from = message.from?.joinToString(),
////                    subject = message.subject ?: "",
////                    body = getEmailBody(message),
////                    to = "",
////                    cc = null,
////                    bcc = null,
////                    account = account[0]
////                )
////            )
////
//////            emailTableQueries.insertEmail(
//////                id = emailUID,
//////                from_user = message.from?.joinToString() ?: "",
//////                subject = message.subject ?: "",
//////                body = getEmailBody(message),
//////                to_user = "",
//////                cc = null,
//////                bcc = null,
//////                account = account[0]
//////            )
////
////            emailCount++
////        }
//
////        for (message in folder.getMessages().takeLast(50)) {
////            val emailUID = uf.getUID(message)
////            println("Message: ${message.from}")
////            emails.add(
////                Email(
////                    id = emailUID,
////                    from = message.from?.joinToString(),
////                    subject = message.subject ?: "",
////                    body = getEmailBody(message),
////                    to = "",
////                    cc = null,
////                    bcc = null,
////                    account = account[0]
////                )
////            )
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
////
////            emailCount++
////        }
//
//        folder.close(false)
//        store.close()
//    }

    @Throws(MessagingException::class)
    fun efficientGetContents(
        emailAddress: String,
        emailTableQueries: EmailsTableQueries,
        emailDataSource: EmailsDataSource,
        accountQueries: AccountsTableQueries,
        store: Store,
        emails: MutableList<EmailsDAO>,
        account: List<Accounts>,
        folder: Folder,
        inbox: IMAPFolder,
        messages: Array<Message>,
        properties: Properties
    ): Int {
        val fp = FetchProfile()
        fp.add(FetchProfile.Item.FLAGS)
        fp.add(FetchProfile.Item.ENVELOPE)
        inbox.fetch(messages, fp)
        val nbMessages = inbox.getMessages().size
        var index = nbMessages - 49
        val maxDoc = 5000
        val maxSize: Long = 100000000 // 100Mo
        totalEmailCount.value = nbMessages

        // Email UIDs
        val uf: UIDFolder = inbox

        // Attachments
        val attachmentDataSource: AttachmentsDataSource = AttachmentsDataSource(db)

        // Emails

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

            val mail = inbox.getMessages()

            end = messages[index - 1].messageNumber
            inbox.doCommand(
                JavaMail(
                    start = nbMessages - 49,
                    end = nbMessages,
                    emails,
                    account,
                    mail,
                    attachments,
                    emailDataSource,
                    attachmentDataSource,
                    emailCount,
                    _emailsRead,
                    folder,
                    uf,
                )
            )

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

    actual fun getEmailCount(emailDataSource: EmailsDataSource): StateFlow<Int> {
//        totalEmailCount = emailDataSource.selectAllEmails().size
        return totalEmails
    }

    actual fun doAttachmentsExist(attachmentsDataSource: AttachmentsDataSource): Boolean {
        val attachments = attachmentsDataSource.selectAllAttachments()
        return attachments.isNotEmpty()
    }

    actual fun returnAttachments(attachmentsDataSource: AttachmentsDataSource): MutableList<AttachmentsDAO> {

        val attach = attachmentsDataSource.selectAllAttachments()
        attachments.addAll(attach)

        return attachments
    }

    fun returnEmails(
        emailTableQueries: EmailsTableQueries,
        emailDataSource: EmailsDataSource
    ): MutableList<EmailsDAO> {
        println("Return emails from database")
        return emailDataSource.selectAllEmails() as MutableList<EmailsDAO>
    }

    actual fun readEmail(
        email: EmailsDAO,
        emailsDataSource: EmailsDataSource,
        emailAddress: String,
    ): Pair<Boolean, Boolean?> {

        val atCred = CredentialManager(emailAddress, "accessToken").returnCredentials()
        val rtCred = CredentialManager(emailAddress, "refreshToken").returnCredentials()
        val idCred = CredentialManager(emailAddress, "idToken").returnCredentials()

        val properties: Properties = Properties().apply {
            put("mail.imap.host", "imap.gmail.com")
            put("mail.imap.auth.mechanisms", "XOAUTH2")
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
                emailAddress,
                atCred?.password.toString()
            )
        }
        val inboxFolder = store.getFolder("INBOX").apply { open(Folder.READ_WRITE) }

        val searchedMessage = inboxFolder.search(MessageIDTerm(email.messageId))

        println("Current read status ${searchedMessage[0].flags.contains(Flags.Flag.SEEN)}")

        return try {
            val readState = !searchedMessage[0].flags.contains(Flags.Flag.SEEN)
            inboxFolder.setFlags(searchedMessage, Flags(Flags.Flag.SEEN), readState)
            inboxFolder.close(true)
            Pair(true, readState)
        } catch (e: Exception) {
            e.printStackTrace()
            inboxFolder.close()
            Pair(false, null)
        }

    }


    actual fun deleteEmail(
        email: EmailsDAO,
        emailsDataSource: EmailsDataSource,
        emailAddress: String,
    ): Boolean {

        val atCred = CredentialManager(emailAddress, "accessToken").returnCredentials()
        val rtCred = CredentialManager(emailAddress, "refreshToken").returnCredentials()
        val idCred = CredentialManager(emailAddress, "idToken").returnCredentials()

        val properties: Properties = Properties().apply {
            put("mail.imap.host", "imap.gmail.com")
            put("mail.imap.auth.mechanisms", "XOAUTH2")
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
                emailAddress,
                atCred?.password.toString()
            )
        }

        val inboxFolder = store.getFolder("INBOX").apply { open(Folder.READ_WRITE) } as IMAPFolder
        val trashFolder = store.getFolder("[Gmail]/Bin").apply { open(Folder.READ_WRITE) }

        val searchedMessage = inboxFolder.search(MessageIDTerm(email.messageId))


        return try {
            inboxFolder.moveMessages(searchedMessage, trashFolder)
            inboxFolder.close(true)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            inboxFolder.close()
            false
        }

    }


    actual fun sendNewEmail(
        emailsDataSource: EmailsDataSource,
        newEmail: NewEmail,
        emailAddress: String,
    ): Boolean {

        val atCred = CredentialManager(emailAddress, "accessToken").returnCredentials()
        val rtCred = CredentialManager(emailAddress, "refreshToken").returnCredentials()
        val idCred = CredentialManager(emailAddress, "idToken").returnCredentials()


        val props = Properties().apply {
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
            put("mail.smtp.auth", true)
//            put("mail.smtp.ssl.enable", true)
            put("mail.smtp.starttls.enable", "true"); //enable STARTTLS
            put("mail.smtp.connectiontimeout", 1000)
            put("mail.smtp.timeout", 3000)
        }

        val auth: Authenticator = object : Authenticator() {
            //override the getPasswordAuthentication method
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(emailAddress, atCred?.password.toString())
            }
        }


        val session = Session.getInstance(props, auth)

        var msg: Message

        try {
            msg = MimeMessage(session).apply {
                setFrom(newEmail.from)
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(newEmail.to))
                setSubject(newEmail.subject, "utf-8")
                setText(newEmail.body, "utf-8")
                setHeader("Content-Type", "text/plain")
            }
        } catch (e: Exception) {
            println("Error creating message $e")
            return false
        }

        val tr = session.getTransport("smtp")

        try {
            Transport.send(msg)
            return true
        } catch (e: Exception) {
            return false
        }

    }

}

actual suspend fun openBrowser(): String {

    val code = CompletableDeferred<String>()

    runKtorServer {
        code.complete(it)
        println("Code: $code")
    }

    val authUrl = "https://accounts.google.com/o/oauth2/v2/auth" +
            "?client_id=113121378086-7s0tvasib3ujgd660d5kkiod7434lp55.apps.googleusercontent.com" +
            "&redirect_uri=http://localhost:8080" +
            "&response_type=code" +
            "&scope=openid%20email%20profile" +
            "&access_type=offline" +    // Request a refresh token
            "&prompt=consent"           // Ensure the consent screen is displayed

    val uri: URI = authUrl.toHttpUrl().toUri()

    val desktop: java.awt.Desktop? =
        if (java.awt.Desktop.isDesktopSupported()) java.awt.Desktop.getDesktop() else null
    if (desktop != null && desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
        try {
            desktop.browse(uri)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    return code.await()
}

actual class Authentication {


    private var code = ""
    private var accessToken = ""
    private var tResponse: TokenResponse? = null
    private var tError: NetworkError? = null
    private var oAuthResponse: OAuthResponse? = null
    private var oAuthErr: NetworkError? = null
    private val deferred = CompletableDeferred<Unit>()

    private val _isLoggedIn = MutableStateFlow(false)
    actual val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _email = MutableStateFlow("")
    actual val email = _email.asStateFlow()

    actual suspend fun authenticateUser(
        fAuthClient: FirebaseAuthClient,
        accountsDataSource: AccountsDataSource
    ): Pair<OAuthResponse?, NetworkError?> {

        runBlocking {
            runKtorServer {
                code = it
                println("Code: $code")
                deferred.complete(Unit)
            }

            val scopes = "openid email profile https://www.googleapis.com/auth/gmail.addons.current.action.compose https://www.googleapis.com/auth/gmail.addons.current.message.action https://www.googleapis.com/auth/gmail.addons.current.message.metadata https://www.googleapis.com/auth/gmail.addons.current.message.metadata https://www.googleapis.com/auth/gmail.send https://www.googleapis.com/auth/gmail.compose https://www.googleapis.com/auth/gmail.metadata"
            val xOAuthScopes = "openid email profile https://mail.google.com/"

            val authUrl = "https://accounts.google.com/o/oauth2/v2/auth" +
                    "?client_id=113121378086-7s0tvasib3ujgd660d5kkiod7434lp55.apps.googleusercontent.com" +
                    "&redirect_uri=http://localhost:8080" +
                    "&response_type=code" +
                    "&scope=$xOAuthScopes" +
                    "&access_type=offline" +    // Request a refresh token
                    "&prompt=consent"           // Ensure the consent screen is displayed

            val uri: URI = authUrl.toHttpUrl().toUri()

            val desktop: java.awt.Desktop? =
                if (java.awt.Desktop.isDesktopSupported()) java.awt.Desktop.getDesktop() else null
            if (desktop != null && desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(uri)
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
            deferred.await()
        }

        if (code.isNotEmpty()) {

            println("Code received: $code")

            fAuthClient.googleTokenIdEndpoint(code).onSuccess {
                tResponse = it
            }.onError {
                tError = it
            }

            if (tError == null && tResponse?.idToken?.isNotEmpty() == true) {
                fAuthClient.googleLogin(tResponse!!.idToken!!).onSuccess {
                    oAuthResponse = it
                    accountsDataSource.insert(
                        null,
                        it.federatedId,
                        it.providerId,
                        it.email,
                        it.emailVerified,
                        it.firstName,
                        it.fullName,
                        it.lastName,
                        it.photoUrl,
                        it.localId,
                        it.displayName,
                        it.expiresIn,
                        it.rawUserInfo,
                        it.kind
                    )
                    CredentialManager(it.email, "accessToken").registerUser(it.email, tResponse!!.accessToken)
                    CredentialManager(it.email, "refreshToken").registerUser(it.email, it.refreshToken)
                    CredentialManager(it.email, "idToken").registerUser(it.email, it.refreshToken)
                    _isLoggedIn.value = true
                }.onError {
                    oAuthErr = it
                }

                if (oAuthErr == null) return Pair(oAuthResponse, null)

            }

            return Pair(null, tError)
        }

        return Pair(null, null)

    }

    actual fun amILoggedIn(accountsDataSource: AccountsDataSource): Boolean {

        val accounts = accountsDataSource.selectAll()

        if (accounts?.isNotEmpty() == true) {
            _isLoggedIn.value = true

            _email.value = accounts[0].email

            val atExists = CredentialManager(accounts[0].email, "accessToken").exists()
            val rfExists = CredentialManager(accounts[0].email, "refreshToken").exists()
            val idExists = CredentialManager(accounts[0].email, "idToken").exists()

            return atExists && rfExists && idExists
        }

        _isLoggedIn.value = false

        return false
    }

    actual fun accountsExists(accountsDataSource: AccountsDataSource): Boolean {
        return accountsDataSource.selectAll().isNotEmpty()
    }

    actual fun getAccounts(accountsDataSource: AccountsDataSource): List<AccountsDAO> {
        return accountsDataSource.selectAll()
    }

    actual fun logout(accountsDataSource: AccountsDataSource, email: String) {
        accountsDataSource.remove(email)
        CredentialManager(email, "accessToken").unregisterUser()
        CredentialManager(email, "refreshToken").unregisterUser()
        CredentialManager(email, "idToken").unregisterUser()
        _isLoggedIn.value = false
    }

    actual fun checkIfTokenExpired(accountsDataSource: AccountsDataSource): Boolean {
        return true
    }

}