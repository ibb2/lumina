package org.example.project.ui.Folder

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.composables.core.*
import org.example.project.data.FolderState
import org.example.project.data.MailFolder
import org.example.project.shared.data.FoldersDAO
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FoldersTabRow(
    folders: MutableList<MailFolder>,
    selectedFolders: MutableState<List<String>>
) {
    val lazyListState = rememberLazyListState()
    val scrollState = rememberScrollAreaState(lazyListState)

    Surface {
        ScrollArea(state = scrollState) {
            LazyRow(
                modifier = Modifier.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                items(folders[0].subFolders) { folder ->
                    FolderItem(
                        folderState = FolderState(folder),
                        selectedFolders = selectedFolders
                    )
                }
            }
            //        HorizontalScrollbar(modifier = Modifier.align(Alignment.BottomEnd).fillMaxWidth()) {
            //            Thumb(
            //                modifier =
            //                    Modifier.background(
            //                        Color.Black.copy(0.3f),
            //                        RoundedCornerShape(100)
            //                    ),
            //                thumbVisibility =
            //                    ThumbVisibility.HideWhileIdle(
            //                        enter = fadeIn(),
            //                        exit = fadeOut(),
            //                        hideDelay = 1.seconds
            //                    )
            //            )
            //        }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderItem(
    folderState: FolderState,
    selectedFolders: MutableState<List<String>>,
    modifier: Modifier = Modifier,
    indentLevel: Int = 0
) {
    Row(
        modifier = modifier.padding(start = (indentLevel * 8).dp, top = 0.dp, bottom = 0.dp),
        verticalAlignment = Alignment.Top
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onClick {
                    val currentFolders = selectedFolders.value.toMutableList()
                    if (currentFolders.contains(folderState.folder.name)) {
                        currentFolders.remove(folderState.folder.name)
                    } else {
                        currentFolders.add(folderState.folder.name)
                    }
                    selectedFolders.value = currentFolders
                },
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = folderState.folder.name,
                textDecoration = if (selectedFolders.value.contains(folderState.folder.name)) TextDecoration.Underline else null,
                style = if (selectedFolders.value.contains(folderState.folder.name)) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "(${folderState.folder.messageCount})",
                style = if (selectedFolders.value.contains(folderState.folder.name)) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium
            )
        }

        if (folderState.folder.subFolders.isNotEmpty()) {
            Icon(
                if (folderState.isExpanded.value) Icons.Outlined.ChevronLeft else Icons.Outlined.ChevronRight,
                contentDescription = "Right Arrow",
                modifier = Modifier.clickable {
                    folderState.isExpanded.value = !folderState.isExpanded.value
                },
            )
        }
        if (folderState.isExpanded.value) {
            folderState.folder.subFolders.forEach { subFolder ->
                FolderItem(
                    folderState = FolderState(subFolder),
                    selectedFolders = selectedFolders,
                    indentLevel = indentLevel + 1
                )
            }
        }
    }
}
