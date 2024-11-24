package org.example.project.ui.dialogs

import org.example.project.EmailService
import org.example.project.data.NewEmail
import org.example.project.sqldelight.EmailsDataSource

class NewEmail {

    private var from = ""
    private var subject = ""
    private var recipient = ""
    private var body = ""

    fun createNewEmail(newEmail: NewEmail) {

        from = newEmail.from
        subject = newEmail.subject
//        recipient = newEmail.recipient
        body = newEmail.body
    }

    fun sendNewEmail(emailService: EmailService, emailsDataSource: EmailsDataSource, emailAddress: String, password: String) {



    }
}