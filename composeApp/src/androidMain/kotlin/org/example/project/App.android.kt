package org.example.project

import com.example.AccountTableQueries
import com.example.EmailTableQueries

actual class EmailService {

    actual fun getEmails(
        emailTableQueries: EmailTableQueries,
        accountQueries: AccountTableQueries,
        emailAddress: String,
        password: String
    ): Array<Email> {
        TODO("Not yet implemented")
    }
}