package org.example.project

import com.example.AccountTableQueries
import com.example.EmailTableQueries
import org.example.project.sqldelight.EmailDataSource

actual class EmailService {

    actual fun getEmails(
        emailDataSource: EmailDataSource,
        emailTableQueries: EmailTableQueries,
        accountQueries: AccountTableQueries,
        emailAddress: String,
        password: String
    ): List<Email> {
        TODO("Not yet implemented")
    }
}