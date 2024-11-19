package org.example.project.shared.data

data class Attachment(
    val id: Int,
    val emailId: Int,
    val fileName: String,
    val mimeType: String,
    val size: Int,
    val downloadPath: String,
    val downloaded: Int
)