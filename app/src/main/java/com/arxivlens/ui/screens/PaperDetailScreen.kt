package com.arxivlens.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arxivlens.data.model.Collection
import com.arxivlens.data.model.Paper
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaperDetailScreen(
    paper: Paper,
    collectionsFlow: StateFlow<List<Collection>>,
    onSave: (collectionId: Long) -> Unit,
    onCreateAndSave: (collectionName: String) -> Unit,
    onNavigateUp: () -> Unit
) {
    val context     = LocalContext.current
    val collections by collectionsFlow.collectAsState()
    var showSaveSheet by remember { mutableStateOf(false) }
    var showNewCollectionDialog by rememberSaveable { mutableStateOf(false) }
    var newCollectionName by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text     = paper.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSaveSheet = true }) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = "Save to collection")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = MaterialTheme.colorScheme.primary,
                    titleContentColor          = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor     = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Title
            item {
                Text(
                    text  = paper.title,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            // Authors
            item {
                Text(
                    text  = paper.authors.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Date + categories
            item {
                Text(
                    text  = "Published ${paper.publishedDate.take(10)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (paper.categories.isNotEmpty()) {
                    FlowRow(
                        modifier             = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        paper.categories.forEach { cat ->
                            AssistChip(
                                onClick = {},
                                label   = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            item { HorizontalDivider() }

            // Abstract
            item {
                Text(
                    text  = "Abstract",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text     = paper.abstract.trim(),
                    style    = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            item { HorizontalDivider() }

            // Action buttons
            item {
                OutlinedButton(
                    onClick  = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(paper.arxivUrl))
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier           = Modifier.padding(end = 8.dp)
                    )
                    Text("View on arXiv")
                }
            }

            if (paper.pdfUrl.isNotBlank()) {
                item {
                    OutlinedButton(
                        onClick  = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(paper.pdfUrl))
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector        = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier           = Modifier.padding(end = 8.dp)
                        )
                        Text("Open PDF")
                    }
                }
            }
        }
    }

    // Save-to-collection bottom sheet
    if (showSaveSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSaveSheet = false },
            sheetState       = sheetState
        ) {
            Text(
                text     = "Save to collection",
                style    = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            )
            TextButton(
                onClick = {
                    newCollectionName = ""
                    showNewCollectionDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("New collection", modifier = Modifier.fillMaxWidth())
            }
            if (collections.isEmpty()) {
                Text(
                    text     = "No collections yet.",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                collections.forEach { collection ->
                    TextButton(
                        onClick  = {
                            onSave(collection.id)
                            showSaveSheet = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(collection.name, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            Box(modifier = Modifier.padding(bottom = 24.dp))
        }
    }

    if (showNewCollectionDialog) {
        AlertDialog(
            onDismissRequest = { showNewCollectionDialog = false },
            title = { Text("New Collection") },
            text = {
                OutlinedTextField(
                    value = newCollectionName,
                    onValueChange = { newCollectionName = it },
                    label = { Text("Collection name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newCollectionName.isNotBlank(),
                    onClick = {
                        onCreateAndSave(newCollectionName)
                        showNewCollectionDialog = false
                        showSaveSheet = false
                        newCollectionName = ""
                    }
                ) {
                    Text("Create & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCollectionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
