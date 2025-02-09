package org.example.project.data

import org.example.project.shared.data.AttachmentsDAO

// Data class returned from getEmailBody
data class EmailContent(
    val plainText: String,
    val htmlText: String,
    val attachments: List<AttachmentsDAO>
)

data class EmailParts(
    var plainText: String = "",
    var htmlText: String = ""
)

// For handeling creating a new Email
data class NewEmail(
    val from: String,
    val subject: String,
    val to: String,
    val cc: Array<String>? = null,
    val bcc: Array<String>? = null,
    val body: String
)