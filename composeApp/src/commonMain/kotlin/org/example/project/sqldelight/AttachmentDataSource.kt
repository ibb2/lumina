package org.example.project.sqldelight

import com.example.project.database.LuminaDatabase

class AttachmentDataSource(db: LuminaDatabase) {

    private val queries = db.attachmentTableQueries

    fun insertAttachment(
        emailId: Long,
        fileName: String,
        mimeType: String,
        size: Long,
        downloadPath: String,
        downloaded: Boolean
    ) = queries.insertAttachment(emailId, fileName, mimeType, size, downloadPath, downloaded)

    fun selectAttachments(emailId: Long) = queries.selectAttachment(emailId)

    fun removeAllAttachments() = queries.removeAllAttachments()

}