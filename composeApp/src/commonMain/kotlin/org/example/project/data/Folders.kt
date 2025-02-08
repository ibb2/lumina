package org.example.project.data

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

data class MailFolder(
    val name: String,
    val messageCount: Int,
    val subFolders: MutableList<MailFolder> = mutableListOf()
)

data class FolderState(
    val folder: MailFolder,
    val isExpanded: MutableState<Boolean> = mutableStateOf(true)
)

