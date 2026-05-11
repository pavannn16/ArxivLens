package com.arxivlens.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arxivlens.data.db.ArxivRepository
import com.arxivlens.data.model.Collection
import com.arxivlens.data.model.CollectionsUiState
import com.arxivlens.ui.viewmodel.CollectionsViewModelFactory
import com.arxivlens.ui.viewmodel.CollectionsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    repository: ArxivRepository,
    onOpenCollection: (Long) -> Unit
) {
    val vm: CollectionsViewModel = viewModel(
        factory = CollectionsViewModelFactory(repository)
    )

    val state            by vm.collectionsState.collectAsState()
    val showCreateDialog by vm.showCreateDialog.collectAsState()
    val pendingDelete    by vm.pendingDelete.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Collections", color = MaterialTheme.colorScheme.onPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = vm::openCreateDialog) {
                Icon(Icons.Default.Add, contentDescription = "New collection")
            }
        }
    ) { innerPadding ->
        Box(
            modifier         = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (val s = state) {
                is CollectionsUiState.Loading -> CircularProgressIndicator()

                is CollectionsUiState.Error -> Text(
                    text  = s.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(24.dp)
                )

                is CollectionsUiState.Success -> {
                    if (s.collections.isEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "No collections yet",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Create a collection to organize saved papers.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier        = Modifier.fillMaxSize(),
                            contentPadding  = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(s.collections, key = { it.id }) { collection ->
                                CollectionRow(
                                    collection  = collection,
                                    repository  = repository,
                                    onClick     = { onOpenCollection(collection.id) },
                                    onDelete    = { vm.requestDelete(collection) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Delete confirmation dialog ────────────────────────────────────
    pendingDelete?.let { collection ->
        AlertDialog(
            onDismissRequest = vm::cancelDelete,
            title   = { Text("Delete collection?") },
            text    = { Text("\"${collection.name}\" and all its saved papers will be removed.") },
            confirmButton = {
                TextButton(onClick = vm::confirmDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = vm::cancelDelete) { Text("Cancel") }
            }
        )
    }

    // ── Create collection dialog ────────────────────────────────────
    if (showCreateDialog) {
        var name by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = vm::closeCreateDialog,
            title   = { Text("New Collection") },
            text    = {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Collection name") },
                    singleLine    = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick  = { vm.createCollection(name) },
                    enabled  = name.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = vm::closeCreateDialog) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CollectionRow(
    collection: Collection,
    repository: ArxivRepository,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val paperCount by produceState(initialValue = 0, collection.id) {
        repository.getPaperCountForCollection(collection.id).collect { value = it }
    }

    Card(
        onClick    = onClick,
        modifier   = Modifier.fillMaxWidth(),
        elevation  = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier             = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment  = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier           = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector        = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text  = collection.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text  = if (paperCount == 1) "1 paper" else "$paperCount papers",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector        = Icons.Default.Delete,
                    contentDescription = "Delete collection",
                    tint               = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
