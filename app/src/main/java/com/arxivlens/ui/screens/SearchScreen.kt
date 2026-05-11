package com.arxivlens.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arxivlens.data.db.ArxivRepository
import com.arxivlens.data.model.Collection
import com.arxivlens.data.model.Paper
import com.arxivlens.data.model.SearchUiState
import com.arxivlens.data.model.SortOption
import com.arxivlens.data.network.ArxivApiService
import com.arxivlens.ui.viewmodel.ArxivViewModelFactory
import com.arxivlens.ui.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    repository: ArxivRepository,
    apiService: ArxivApiService,
    onNavigateToCollections: () -> Unit,
    onOpenPaper: (Paper) -> Unit
) {
    val vm: SearchViewModel = viewModel(
        factory = ArxivViewModelFactory(repository, apiService)
    )

    val displayState by vm.displayState.collectAsState()
    val currentSort  by vm.sortOption.collectAsState()
    val query        by vm.query.collectAsState()
    val collections  by vm.collections.collectAsState()
    val pendingPaper by vm.pendingPaper.collectAsState()
    val saveMessage  by vm.saveMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showSortMenu by remember { mutableStateOf(false) }
    var showNewCollectionDialog by rememberSaveable { mutableStateOf(false) }
    var newCollectionName by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(saveMessage) {
        saveMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.onSaveMessageShown()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        }
    ) { scaffoldPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(scaffoldPadding)) {

        // ── Branded header + search bar ───────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
        ) {
            // Title row with sort button
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = "ArxivLens",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                // Sort dropdown — visible only when results are showing
                if (displayState is SearchUiState.Success) {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector        = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort results",
                                tint               = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        DropdownMenu(
                            expanded         = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text    = { Text(option.label) },
                                    onClick = {
                                        vm.setSortOption(option)
                                        showSortMenu = false
                                    },
                                    trailingIcon = if (currentSort == option) {
                                        { Icon(Icons.Default.Check, contentDescription = null) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
            OutlinedTextField(
                value         = query,
                onValueChange = vm::onQueryChange,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                placeholder     = { Text("Search arXiv…", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)) },
                singleLine      = true,
                colors          = OutlinedTextFieldDefaults.colors(
                    focusedTextColor       = MaterialTheme.colorScheme.onPrimary,
                    unfocusedTextColor     = MaterialTheme.colorScheme.onPrimary,
                    focusedBorderColor     = MaterialTheme.colorScheme.onPrimary,
                    unfocusedBorderColor   = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                    cursorColor            = MaterialTheme.colorScheme.onPrimary,
                    focusedTrailingIconColor   = MaterialTheme.colorScheme.onPrimary,
                    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                ),
                trailingIcon    = {
                    IconButton(onClick = {
                        keyboard?.hide()
                        vm.search()
                    }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboard?.hide()
                    vm.search()
                })
            )
        }

        // ── Content area (weight fills remaining height — required for LazyColumn) ──
        Box(
            modifier            = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment    = Alignment.Center
        ) {
            when (val state = displayState) {
                is SearchUiState.Idle -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "Find research papers",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Search by keyword, author, or topic.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is SearchUiState.Loading -> {
                    CircularProgressIndicator()
                }

                is SearchUiState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier            = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text  = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = { vm.search() }) {
                            Text("Retry")
                        }
                    }
                }

                is SearchUiState.Success -> {
                    LazyColumn(
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.papers, key = { it.id }) { paper ->
                            PaperCard(
                                paper   = paper,
                                onClick = { vm.selectPaper(paper); onOpenPaper(paper) },
                                onSave  = { vm.onSavePaperClick(paper) }
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Save-to-collection bottom sheet ───────────────────────────────────
    val paper = pendingPaper
    if (paper != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = vm::dismissSaveDialog,
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
                        onClick  = { vm.savePaper(paper, collection.id) },
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

    if (paper != null && showNewCollectionDialog) {
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
                        vm.savePaperToNewCollection(paper, newCollectionName)
                        showNewCollectionDialog = false
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
    } // Scaffold content
} // SearchScreen
