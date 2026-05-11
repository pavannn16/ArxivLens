package com.arxivlens.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arxivlens.data.db.ArxivRepository
import com.arxivlens.data.model.CollectionDetailUiState
import com.arxivlens.data.model.SavedPaper
import com.arxivlens.ui.viewmodel.CollectionDetailViewModelFactory
import com.arxivlens.ui.viewmodel.CollectionDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    repository: ArxivRepository,
    collectionId: Long,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    val vm: CollectionDetailViewModel = viewModel(
        factory = CollectionDetailViewModelFactory(repository, collectionId)
    )

    val uiState    by vm.uiState.collectAsState()
    val shareEvent by vm.shareEvent.collectAsState()

    // Launch the system share sheet when an export Intent is ready
    val shareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { vm.onShareEventConsumed() }

    LaunchedEffect(shareEvent) {
        shareEvent?.let {
            shareLauncher.launch(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = (uiState as? CollectionDetailUiState.Success)?.collection?.name ?: ""
                    Text(title)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor         = MaterialTheme.colorScheme.primary,
                    titleContentColor      = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (uiState is CollectionDetailUiState.Success) {
                val papers = (uiState as CollectionDetailUiState.Success).papers
                if (papers.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = { vm.exportBibTex(context) },
                        icon    = { Icon(Icons.Default.Share, contentDescription = null) },
                        text    = { Text("Export BibTeX") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier         = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (val s = uiState) {
                is CollectionDetailUiState.Loading -> CircularProgressIndicator()

                is CollectionDetailUiState.Error -> Text(
                    text     = s.message,
                    color    = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(24.dp)
                )

                is CollectionDetailUiState.Success -> {
                    if (s.papers.isEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "No papers saved",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Saved papers in this collection will appear here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier        = Modifier.fillMaxSize(),
                            contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(s.papers, key = { it.paperId }) { paper ->
                                SavedPaperRow(
                                    paper    = paper,
                                    onOpen   = {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(paper.arxivUrl))
                                        )
                                    },
                                    onRemove = { vm.removePaper(paper.paperId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedPaperRow(
    paper: SavedPaper,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        onClick   = onOpen,
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier       = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(end = 88.dp)) {
                Text(
                    text     = paper.title,
                    style    = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val authorsText = paper.authors
                    .split(",")
                    .take(3)
                    .joinToString(", ")
                    .let { if (paper.authors.split(",").size > 3) "$it et al." else it }
                Text(
                    text     = authorsText,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text     = paper.publishedDate.take(4),
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                IconButton(onClick = onOpen) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open on arXiv",
                        tint               = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector        = Icons.Default.Delete,
                        contentDescription = "Remove paper",
                        tint               = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
