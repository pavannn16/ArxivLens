package com.arxivlens.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arxivlens.ArxivLensApp
import com.arxivlens.ui.screens.CollectionDetailScreen
import com.arxivlens.ui.screens.CollectionsScreen
import com.arxivlens.data.model.Paper
import com.arxivlens.ui.screens.PaperDetailScreen
import com.arxivlens.ui.screens.SearchScreen
import com.arxivlens.ui.viewmodel.ArxivViewModelFactory
import com.arxivlens.ui.viewmodel.SearchViewModel

// ── Route constants ──────────────────────────────────────────────────────────

object Routes {
    const val SEARCH             = "search"
    const val COLLECTIONS        = "collections"
    const val COLLECTION_DETAIL  = "collection_detail/{collectionId}"
    const val PAPER_DETAIL       = "paper_detail"

    fun collectionDetail(collectionId: Long) = "collection_detail/$collectionId"
}

// ── Bottom nav items ─────────────────────────────────────────────────────────

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.SEARCH,      "Search",      Icons.Default.Search),
    BottomNavItem(Routes.COLLECTIONS, "Collections", Icons.Default.Collections),
)

// ── Root NavHost ─────────────────────────────────────────────────────────────

@Composable
fun ArxivLensNavGraph(app: ArxivLensApp) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon  = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Routes.SEARCH,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Routes.SEARCH) {
                // Hoist the VM here so we can capture it in the onOpenPaper callback
                val searchVm: SearchViewModel = viewModel(
                    factory = ArxivViewModelFactory(app.repository, app.apiService)
                )
                SearchScreen(
                    repository  = app.repository,
                    apiService  = app.apiService,
                    onNavigateToCollections = {
                        navController.navigate(Routes.COLLECTIONS)
                    },
                    onOpenPaper = { paper ->
                        searchVm.selectPaper(paper)
                        navController.navigate(Routes.PAPER_DETAIL)
                    }
                )
            }

            composable(Routes.PAPER_DETAIL) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Routes.SEARCH)
                }
                val searchVm: SearchViewModel = viewModel(
                    parentEntry,
                    factory = ArxivViewModelFactory(app.repository, app.apiService)
                )
                val paper by searchVm.selectedPaper.collectAsState()
                paper?.let {
                    PaperDetailScreen(
                        paper          = it,
                        collectionsFlow = searchVm.collections,
                        onSave         = { collectionId -> searchVm.savePaper(it, collectionId) },
                        onCreateAndSave = { name -> searchVm.savePaperToNewCollection(it, name) },
                        onNavigateUp   = {
                            searchVm.clearSelectedPaper()
                            navController.navigateUp()
                        }
                    )
                }
            }

            composable(Routes.COLLECTIONS) {
                CollectionsScreen(
                    repository = app.repository,
                    onOpenCollection = { collectionId ->
                        navController.navigate(Routes.collectionDetail(collectionId))
                    }
                )
            }

            composable(
                route     = Routes.COLLECTION_DETAIL,
                arguments = listOf(navArgument("collectionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val collectionId = backStackEntry.arguments?.getLong("collectionId") ?: return@composable
                CollectionDetailScreen(
                    repository   = app.repository,
                    collectionId = collectionId,
                    onNavigateUp = { navController.navigateUp() }
                )
            }
        }
    }
}
