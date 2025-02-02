package org.example.project.ui.Folder

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.onClick
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import org.example.project.shared.data.FoldersDAO

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FoldersTabRow(folders: MutableList<FoldersDAO>, selectedFolders: MutableState<List<String>>) {

    LazyRow(modifier = Modifier) {
        itemsIndexed(folders) { _, it ->
            Row() {
                val isSelected = selectedFolders.value.contains(it.name)
                val color = if (isSelected) Color.Unspecified else Color.Gray

                Text(
                    modifier = Modifier.onClick(
                        onClick = {
                            println("Folder selected ${it.name}")
                            val currentFolders =
                                selectedFolders.value.toMutableList()
                            if (currentFolders.contains(it.name)) {
                                currentFolders.remove(it.name)
                            } else {
                                currentFolders.add(it.name)
                            }
                            // Update the entire list
                            selectedFolders.value = currentFolders
                        }
                    ),
                    textDecoration = if (isSelected) TextDecoration.Underline else null,
                    color = color,
                    text = it.name
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
        }
    }
}