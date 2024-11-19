package org.example.project

import com.example.AccountTableQueries
import com.example.EmailTableQueries
import kotlinx.coroutines.flow.StateFlow
import org.example.project.shared.data.EmailDAO
import org.example.project.sqldelight.EmailDataSource

actual class EmailService {

    actual suspend fun getEmails(
        emailDataSource: EmailDataSource,
        emailTableQueries: EmailTableQueries,
        accountQueries: AccountTableQueries,
        emailAddress: String,
        password: String
    ): List<EmailDAO> {
        TODO("Not yet implemented")
    }

    actual val emailsRead: StateFlow<Int>
        get() = TODO("Not yet implemented")
    actual var emailCount: Int
        get() = TODO("Not yet implemented")
        set(value) {}

    actual suspend fun deleteEmails(emailDataSource: EmailDataSource) {
    }

    actual fun returnEmails(
        emailTableQueries: EmailTableQueries,
        emailDataSource: EmailDataSource
    ): List<EmailDAO> {
        TODO("Not yet implemented")
    }

    actual fun getEmailCount(emailDataSource: EmailDataSource): Int {
        TODO("Not yet implemented")
    }
}