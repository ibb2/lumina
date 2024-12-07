package org.example.project.sqldelight.repositories

import com.example.project.database.LuminaDatabase

class Folders(db: LuminaDatabase) {

    private val queries = db.foldersTableQueries

    fun insertFolder(folderId: String, folderName: String) = queries.insertFolder(null, folderId, folderName)

    fun insertOrReplaceFolder(folderId: String, folderName: String) = queries.insertOrReplaceFolder(null, folderId, folderName)

    fun getFolders() = queries.selectAllFolders().executeAsList()

    fun getFolder(id: Long) = queries.selectFolder(id).executeAsOne()
}