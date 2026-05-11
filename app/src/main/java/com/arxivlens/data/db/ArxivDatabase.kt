package com.arxivlens.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.arxivlens.data.model.Collection
import com.arxivlens.data.model.SavedPaper

/**
 * Single Room database for ArxivLens.
 *
 * Two tables:
 *   collections  — named folders the user creates
 *   saved_papers — papers stored inside those folders (FK → collections)
 *
 * Increment [version] and provide a [Migration] whenever the schema changes.
 */
@Database(
    entities = [Collection::class, SavedPaper::class],
    version = 1,
    exportSchema = false
)
abstract class ArxivDatabase : RoomDatabase() {

    abstract fun collectionDao(): CollectionDao
    abstract fun savedPaperDao(): SavedPaperDao

    companion object {
        @Volatile
        private var INSTANCE: ArxivDatabase? = null

        /**
         * Returns the singleton database instance, creating it on first call.
         *
         * Using a double-checked lock ensures only one instance is ever created
         * even when [getDatabase] is called from multiple coroutines simultaneously.
         */
        fun getDatabase(context: Context): ArxivDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ArxivDatabase::class.java,
                    "arxivlens.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)  // safe during dev
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
