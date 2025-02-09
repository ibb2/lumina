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