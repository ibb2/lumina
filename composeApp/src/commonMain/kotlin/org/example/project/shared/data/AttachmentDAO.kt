package org.example.project.shared.data

data class AttachmentDAO(
    val id: Long,
    val emailId: Long,
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val downloadPath: String,
    val downloaded: Int
)