package org.example.project

import com.example.AccountsTableQueries
import com.example.EmailsTableQueries
import kotlinx.coroutines.flow.StateFlow
import org.example.project.data.NewEmail
import org.example.project.shared.data.AttachmentsDAO
import org.example.project.shared.data.EmailsDAO
import org.example.project.sqldelight.AttachmentsDataSource
import org.example.project.sqldelight.EmailsDataSource

actual class EmailService {

    actual suspend fun getEmails(
        emailDataSource: EmailsDataSource,
        emailTableQueries: EmailsTableQueries,
        accountQueries: AccountsTableQueries,
        emailAddress: String,
        password: String
    ): Pair<MutableList<EmailsDAO>, MutableList<AttachmentsDAO>> {
        TODO("Not yet implemented")
    }


    actual val emailsRead: StateFlow<Int>
        get() = TODO("Not yet implemented")
    actual var emailCount: Int
        get() = TODO("Not yet implemented")
        set(value) {}

    actual suspend fun deleteEmails(emailDataSource: EmailsDataSource) {
    }

    actual fun returnAttachments(attachmentsDataSource: AttachmentsDataSource): MutableList<AttachmentsDAO> {
        TODO("Not yet implemented")
    }

    actual fun doAttachmentsExist(attachmentsDataSource: AttachmentsDataSource): Boolean {
        TODO("Not yet implemented")
    }

    actual val totalEmails: StateFlow<Int>
        get() = TODO("Not yet implemented")

    actual fun getEmailCount(emailDataSource: EmailsDataSource): StateFlow<Int> {
        TODO("Not yet implemented")
    }

    actual fun readEmail(
        email: EmailsDAO,
        emailsDataSource: EmailsDataSource,
        emailAddress: String,
        password: String
    ): Pair<Boolean, Boolean?> {
        TODO("Not yet implemented")
    }

    actual fun deleteEmail(
        email: EmailsDAO,
        emailsDataSource: EmailsDataSource,
        emailAddress: String,
        password: String
    ): Boolean {
        TODO("Not yet implemented")
    }

    actual fun sendNewEmail(
        emailsDataSource: EmailsDataSource,
        newEmail: NewEmail,
        emailAddress: String,
        password: String
    ): Boolean {
        TODO("Not yet implemented")
    }
}

actual fun openBrowser(): String {
}