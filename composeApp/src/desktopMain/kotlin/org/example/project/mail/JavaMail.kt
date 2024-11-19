package org.example.project.mail

import jakarta.mail.Message
import jakarta.mail.MessagingException
import jakarta.mail.Session
import jakarta.mail.UIDFolder
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jdk.javadoc.internal.doclets.formats.html.Contents
import org.eclipse.angus.mail.iap.Argument
import org.eclipse.angus.mail.iap.Response
import org.eclipse.angus.mail.imap.IMAPFolder
import org.eclipse.angus.mail.imap.protocol.BODY
import org.eclipse.angus.mail.imap.protocol.FetchResponse
import org.eclipse.angus.mail.imap.protocol.IMAPProtocol
import org.eclipse.angus.mail.imap.protocol.IMAPResponse
import org.example.project.shared.data.EmailsDAO
import java.io.ByteArrayInputStream
import java.util.*
import javax.management.remote.JMXConnectorFactory.connect

class JavaMail(
    /** Index on server of first mail to fetch  */
    var start: Int,
    /** Index on server of last mail to fetch  */
    var end: Int,
    var emails: MutableList<EmailsDAO>,
    var uf: UIDFolder,
    var properties: Properties,
    var account: List<String>,
    var messages: Array<Message>
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

//                        emails.add(
//                            Email(
//                                id = 1,
//                                from = mm.from.toString(),
//                                subject = mm.subject,
//                                body = getEmailBody(mm, i),
//                                to = mm.allRecipients[0].toString(),
//                                cc = if (mm.allRecipients.size > 1) mm.allRecipients[1].toString() else "",
//                                bcc = if (mm.allRecipients.size > 2) mm.allRecipients[2].toString() else "",
//                                account = account[0]
//                            )
//                        )
//                      getContents(mm, i, emails, uf)
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

    fun getEmailBody(mm: MimeMessage, i: Int): String {
        return when (val content = mm.content) {
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