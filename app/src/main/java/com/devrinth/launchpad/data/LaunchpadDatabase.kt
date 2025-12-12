package com.devrinth.launchpad.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for Launchpad app.
 * Contains clipboard history table.
 */
@Database(
    entities = [ClipboardEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LaunchpadDatabase : RoomDatabase() {

    abstract fun clipboardDao(): ClipboardDao

    companion object {
        @Volatile
        private var INSTANCE: LaunchpadDatabase? = null

        private const val DATABASE_NAME = "launchpad_database"

        /**
         * Get the singleton database instance.
         */
        fun getInstance(context: Context): LaunchpadDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LaunchpadDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
