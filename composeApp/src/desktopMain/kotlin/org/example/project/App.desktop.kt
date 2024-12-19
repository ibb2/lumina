package org.example.project

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import com.example.Accounts
import com.example.Emails
import com.example.project.database.LuminaDatabase
import com.konyaco.fluent.FluentTheme
import com.konyaco.fluent.background.Mica
import com.konyaco.fluent.darkColors
import com.konyaco.fluent.lightColors
import jakarta.mail.*
import jakarta.mail.event.MessageCountAdapter
import jakarta.mail.event.MessageCountEvent
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import jakarta.mail.search.MessageIDTerm
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.eclipse.angus.mail.imap.IMAPFolder
import org.example.project.data.NewEmail
import org.example.project.mail.JavaMail
import org.example.project.networking.*
import org.example.project.shared.data.AccountsDAO
import org.example.project.shared.data.AttachmentsDAO
import org.example.project.shared.data.EmailsDAO
import org.example.project.shared.data.FoldersDAO
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


actual class EmailService actual constructor(
    private val client: FirebaseAuthClient,
) {

    private var db: LuminaDatabase
    private var emailDataSource: EmailsDataSource
    private var attachmentsDataSource: AttachmentsDataSource
    private var accountsDataSource: AccountsDataSource

    private val _folders = MutableStateFlow<MutableList<FoldersDAO>>(mutableListOf())
    actual val folders = _folders.asStateFlow()

    private val _emails = MutableStateFlow<MutableList<EmailsDAO>>(mutableListOf())
    actual val emails = _emails.asStateFlow()

    private val _attachments = MutableStateFlow<MutableList<AttachmentsDAO>>(mutableListOf())
    actual val attachments = _attachments.asStateFlow()

    private val _isSyncing = MutableStateFlow<Boolean>(false)
    actual val isSyncing = _isSyncing.asStateFlow()

    init {
        val driver = DatabaseDriverFactory().create()

        db = LuminaDatabase(
            driver,
            EmailsAdapter = Emails.Adapter(
                attachments_countAdapter = IntColumnAdapter
            )
        )

        // Initialising the data sources
        emailDataSource = EmailsDataSource(db)
        attachmentsDataSource = AttachmentsDataSource(db)
        accountsDataSource = AccountsDataSource(db)
    }

    private fun getProps(): Properties {
        // Return properties need for Session

        return Properties().apply {
            put("mail.imap.host", "imap.gmail.com")
            put("mail.imap.auth.mechanisms", "XOAUTH2");
            put("mail.imap.port", "993")
            put("mail.imap.ssl.enable", "true")
            put("mail.imap.connectiontimeout", 10000)
            put("mail.imap.timeout", 10000)
            put("mail.imap.partialfetch", "false");
            put("mail.imap.fetchsize", "1048576");
        }
    }

    private suspend fun connectToStore(session: Session, props: Properties, emailAddress: String): Store {
        // Connect to the Imap store session

        try {
            val atCred = CredentialManager(emailAddress, "accessToken").returnCredentials()

            val password = String(atCred?.password!!)

            return session.getStore("imap").apply {
                connect(
                    props.getProperty("mail.imap.host"),
                    emailAddress,
                    password
                )
            }
        } catch (e: Exception) {

            var results: DjangoRefreshTokenResponse? = null
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
            CredentialManager(emailAddress, "idToken").registerUser(emailAddress, results!!.idToken)


            return session.getStore("imap").apply {
                connect(
                    props.getProperty("mail.imap.host"),
                    emailAddress,
                    results!!.accessToken
                )
            }
        }
    }

    private fun checkForNewEmails(emailAddress: String, messagesSize: Int): Boolean {
        // Check if emails exist in db
        println("Checking for emails and attachments...")

        val totalEmailsForAccount = emailDataSource.selectAllEmailsForAccount(emailAddress).size
        println("Email $emailAddress Size $totalEmailsForAccount")
        return if (totalEmailsForAccount > 0) totalEmailsForAccount < messagesSize else false
    }

    actual suspend fun getFolders(emailAddress: String): MutableList<FoldersDAO> {

        val props = getProps()
        val session = Session.getInstance(props)
        val store = connectToStore(session, props, emailAddress)

        val localFolders: MutableList<FoldersDAO> = mutableListOf()

        // Display the folders
        println("Showing folders")
        val folders = store.defaultFolder.list("*")
        folders.map {
            println("Folder: ${it.name}")
            try {
                localFolders.add(
                    FoldersDAO(
                        null,
                        "$emailAddress|${it.name}",
                        it.name
                    )
                )
            } catch (e: Exception) {
                println("Folder already exists in database")
            }
        }

        return localFolders
    }

    actual suspend fun getEmails(
        emailAddress: String
    ): Pair<StateFlow<List<EmailsDAO>>, StateFlow<List<AttachmentsDAO>>> {

        val localEmails = MutableStateFlow<List<EmailsDAO>>(emptyList())
        val localAttachments = MutableStateFlow<List<AttachmentsDAO>>(emptyList())

        val props = getProps()
        val session = Session.getInstance(props)

        val store = connectToStore(session, props, emailAddress)
        val inbox = store.getFolder("INBOX").apply { open(Folder.READ_ONLY) } as IMAPFolder
        val messages = inbox.getMessages()

        // Check if emails exist in db
        println("Checking for emails and attachments...")
        val emailsUpToDate = checkForNewEmails(emailAddress, messages.size)

        val account = accountsDataSource.select(emailAddress)

        if (emailsUpToDate) {

            println("Retrieving emails and attachments from database...")
            val dbEmails = emailDataSource.selectAllEmailsForAccount(emailAddress)
            localEmails.value = dbEmails

            if (messages.size > dbEmails.size) {
                println("Found new emails in IMAP server updating database...")

                // Safely get new messages
                val startIndex = maxOf(messages.size - dbEmails.size - 1, messages.size)
                val newMsgs = inbox.getMessages(messages.size, messages.size)

                println("New messages ${newMsgs.size}")

                // Safely pass parameters to efficientGetContents
//                efficientGetContents(
//                    account,
//                    inbox,
//                    newMsgs,
//                    startNo = newMsgs.firstOrNull()?.messageNumber,
//                    endNo = newMsgs.lastOrNull()?.messageNumber
//                )

                inbox.doCommand(
                    JavaMail(
                        start = messages.size - 1,
                        end = messages.size,
                        _emails,
                        _attachments,
                        emailDataSource,
                        attachmentsDataSource,
                        account,
                        inbox.getMessages()
                    )
                )

            }

            val dbAttachments = dbEmails.firstOrNull()?.let {
                attachmentsDataSource.selectAttachments(it.id!!)
            } ?: emptyList()
            localAttachments.value = dbAttachments

            _emails.value = localEmails.value.toMutableList()
            _attachments.value = localAttachments.value.toMutableList()

            return Pair(localEmails, localAttachments)
        }

        println("Retrieving emails and attachments from IMAP server...")

        efficientGetContents(
            account,
            inbox,
            messages,
        )

        inbox.close(false)
        store.close()

        return Pair(emails, attachments)
    }

    actual suspend fun watchEmails(emailAddress: String, dataSource: EmailsDataSource) {
        coroutineScope {
            launch(Dispatchers.IO) {
                val props = getProps()
                val session = Session.getInstance(props)
                val store = connectToStore(session, props, emailAddress)
                val inbox = store.getFolder("INBOX").apply { open(Folder.READ_ONLY) } as IMAPFolder

                val account = accountsDataSource.select(emailAddress)

                inbox.addMessageCountListener(object : MessageCountAdapter() {
                    override fun messagesAdded(ev: MessageCountEvent) {
                        launch {
                            val msgs = ev.messages
                            println("New messages detected: ${msgs.size}")

                            try {
                                inbox.doCommand(
                                    JavaMail(
                                        start = msgs[0].messageNumber,
                                        end = msgs.last().messageNumber,
                                        _emails,
                                        _attachments,
                                        dataSource,
                                        attachmentsDataSource,
                                        account,
                                        inbox.getMessages()
                                    )
                                )

                                println("Database updated")
                            } catch (e: Exception) {
                                println("Error processing new messages: ${e.printStackTrace()}")
                            }
                        }
                    }
                })

                // Maintain IDLE connection with reconnection logic
                while (isActive) {
                    try {
                        inbox.idle()
                    } catch (e: Exception) {
                        println("IDLE connection lost, reconnecting in 5 seconds...")
                        delay(5000)
                    }
                }
            }
        }
    }

    @Throws(MessagingException::class)
    fun efficientGetContents(
        account: Accounts,
        inbox: IMAPFolder,
        messages: Array<Message>,
        startNo: Int? = null,
        endNo: Int? = null
    ): Int {
        val fp = FetchProfile()
        fp.add(FetchProfile.Item.FLAGS)
        fp.add(FetchProfile.Item.ENVELOPE)
        inbox.fetch(messages, fp)

        val nbMessages = inbox.getMessages().size
        var index = startNo ?: (nbMessages - 10)
        val maxDoc = 5000
        val maxSize: Long = 100000000 // 100Mo

        // Message numbers limit to fetch
        var start: Int
        var end: Int

        while (index <= nbMessages) {
            start = startNo ?: messages[index].messageNumber
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
                    noskip = (messages[index - 1].messageNumber + 1 === messages[index].messageNumber)
                }
            }

            val mail = inbox.getMessages()

            end = endNo ?: messages[index - 1].messageNumber
            inbox.doCommand(
                JavaMail(
                    start = startNo ?: (nbMessages - 10),
                    end = nbMessages,
                    _emails,
                    _attachments,
                    emailDataSource,
                    attachmentsDataSource,
                    account,
                    mail,
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

    actual suspend fun searchEmails(query: String): List<EmailsDAO> {

        val matchingEmails = MutableStateFlow<MutableList<EmailsDAO>>(mutableListOf())

        val search = emailDataSource.search(query)
        println("Function $search")
        return search
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


actual class Authentication {


    private var code = ""
    private var accessToken = ""
    private var refreshToken = ""
    private var idToken = ""
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
            val server = runKtorServer {
                code = it
                println("Code: $code")
                deferred.complete(Unit)
            }

            server.start()

            val scopes =
                "openid email profile https://www.googleapis.com/auth/gmail.addons.current.action.compose https://www.googleapis.com/auth/gmail.addons.current.message.action https://www.googleapis.com/auth/gmail.addons.current.message.metadata https://www.googleapis.com/auth/gmail.addons.current.message.metadata https://www.googleapis.com/auth/gmail.send https://www.googleapis.com/auth/gmail.compose https://www.googleapis.com/auth/gmail.metadata"
            val xOAuthScopes = "openid email profile https://mail.google.com/"

            val authUrl = "https://accounts.google.com/o/oauth2/v2/auth" +
                    "?client_id=113121378086-7s0tvasib3ujgd660d5kkiod7434lp55.apps.googleusercontent.com" +
                    "&redirect_uri=http://localhost:3000" +
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
            server.stop()
        }

        if (code.isNotEmpty()) {
            println("Code received: $code")

            fAuthClient.googleTokenIdEndpoint(code).onSuccess {
                tResponse = it
                println("Initial refresh token ${it.refreshToken}")
                accessToken = it.accessToken
                refreshToken = it.refreshToken!!
                idToken = it.idToken!!
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
                    println("Refresh token ${it.refreshToken}")
                    CredentialManager(it.email, "accessToken").registerUser(it.email, accessToken)
                    CredentialManager(it.email, "refreshToken").registerUser(it.email, refreshToken)
                    CredentialManager(it.email, "idToken").registerUser(it.email, idToken)
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

    actual fun getAccounts(accountsDataSource: AccountsDataSource): MutableList<AccountsDAO> {
        return accountsDataSource.selectAll().toMutableList()
    }

    actual fun logout(accountsDataSource: AccountsDataSource, email: String) {
        accountsDataSource.remove(email)
        CredentialManager(email, "accessToken").unregisterUser()
        CredentialManager(email, "refreshToken").unregisterUser()
        CredentialManager(email, "idToken").unregisterUser()
        if (!accountsExists(accountsDataSource)) _isLoggedIn.value = false
    }

    actual fun checkIfTokenExpired(accountsDataSource: AccountsDataSource): Boolean {
        return true
    }

}

@Composable
actual fun PlatformSpecificUI(
    modifier: Modifier,
    currentSystemTheme: Boolean,
    content: @Composable () -> Unit,
) {

    FluentTheme( colors = if (currentSystemTheme) darkColors() else lightColors()) {
        Mica(modifier.fillMaxSize()) {
            content()
        }
    }

}