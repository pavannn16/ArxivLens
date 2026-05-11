package com.arxivlens

import android.app.Application
import com.arxivlens.data.db.ArxivDatabase
import com.arxivlens.data.db.ArxivRepository
import com.arxivlens.data.network.ArxivApiService

/**
 * Application subclass that creates the single instances of the database,
 * repository, and API service at app startup.
 *
 * ViewModels access these via the [ArxivViewModelFactory] (Phase 6).
 */
class ArxivLensApp : Application() {

    val database: ArxivDatabase by lazy { ArxivDatabase.getDatabase(this) }
    val repository: ArxivRepository by lazy { ArxivRepository(database) }
    val apiService: ArxivApiService by lazy { ArxivApiService() }
}
