package org.example.project

import com.example.AccountsTableQueries
import com.example.EmailsTableQueries
import kotlinx.coroutines.flow.StateFlow
import org.example.project.shared.data.EmailsDAO
import org.example.project.sqldelight.EmailsDataSource

actual class EmailService {

    actual suspend fun getEmails(
        emailDataSource: EmailsDataSource,
        emailTableQueries: EmailsTableQueries,
        accountQueries: AccountsTableQueries,
        emailAddress: String,
        password: String
    ): List<EmailsDAO> {
        TODO("Not yet implemented")
    }

    actual val emailsRead: StateFlow<Int>
        get() = TODO("Not yet implemented")
    actual var emailCount: Int
        get() = TODO("Not yet implemented")
        set(value) {}

    actual suspend fun deleteEmails(emailDataSource: EmailsDataSource) {
    }

    actual fun returnEmails(
        emailTableQueries: EmailsTableQueries,
        emailDataSource: EmailsDataSource
    ): List<EmailsDAO> {
        TODO("Not yet implemented")
    }

    actual fun getEmailCount(emailDataSource: EmailsDataSource): Int {
        TODO("Not yet implemented")
    }
}