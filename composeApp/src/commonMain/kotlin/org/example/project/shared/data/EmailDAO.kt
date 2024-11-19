package org.example.project.shared.data

data class EmailDAO(
    val id: Long,
    val compositeKey: String,
    val folderName: String,
    val subject: String,
    val sender: String,
    val recipients: ByteArray,
    val sentDate: String,
    val receivedDate: String,
    val body: String,
    val snippet: String,
    val size: Long,
    val isRead: Boolean,
    val isFlagged: Boolean,
    val attachmentsCount: Int,
    val hasAttachments: Boolean,
    val account: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as EmailDAO

        if (id != other.id) return false
        if (size != other.size) return false
        if (isRead != other.isRead) return false
        if (isFlagged != other.isFlagged) return false
        if (attachmentsCount != other.attachmentsCount) return false
        if (hasAttachments != other.hasAttachments) return false
        if (compositeKey != other.compositeKey) return false
        if (folderName != other.folderName) return false
        if (subject != other.subject) return false
        if (sender != other.sender) return false
        if (!recipients.contentEquals(other.recipients)) return false
        if (sentDate != other.sentDate) return false
        if (receivedDate != other.receivedDate) return false
        if (body != other.body) return false
        if (snippet != other.snippet) return false
        if (account != other.account) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + isRead.hashCode()
        result = 31 * result + isFlagged.hashCode()
        result = 31 * result + attachmentsCount
        result = 31 * result + hasAttachments.hashCode()
        result = 31 * result + compositeKey.hashCode()
        result = 31 * result + folderName.hashCode()
        result = 31 * result + subject.hashCode()
        result = 31 * result + sender.hashCode()
        result = 31 * result + recipients.contentHashCode()
        result = 31 * result + sentDate.hashCode()
        result = 31 * result + receivedDate.hashCode()
        result = 31 * result + body.hashCode()
        result = 31 * result + snippet.hashCode()
        result = 31 * result + account.hashCode()
        return result
    }
}
