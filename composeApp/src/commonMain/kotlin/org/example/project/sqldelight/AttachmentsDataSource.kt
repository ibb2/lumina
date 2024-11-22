package org.example.project.sqldelight

import com.example.project.database.LuminaDatabase
import org.example.project.shared.data.AttachmentsDAO

class AttachmentsDataSource(db: LuminaDatabase) {

    private val queries = db.attachmentsTableQueries

    fun insertAttachment(
        id: Long? = null,
        emailId: Long,
        fileName: String,
        mimeType: String,
        size: Long,
        downloadPath: String = "",
        downloaded: Boolean = false
    ) = queries.insertAttachment(id, emailId, fileName, mimeType, size, downloadPath, downloaded)

    fun selectAttachments(emailId: Long) = queries.selectAttachment(emailId)

    fun removeAllAttachments() = queries.removeAllAttachments()

    fun selectAllAttachments(): List<AttachmentsDAO> = queries.selectAllAttachments(
        mapper = { id, emailId, fileName, mimeType, size, downloadPath, downloaded -> AttachmentsDAO(id, emailId, fileName, mimeType ?: "", size ?: 0, downloadPath ?:"", downloaded ?: false) }
    ).executeAsList()
}