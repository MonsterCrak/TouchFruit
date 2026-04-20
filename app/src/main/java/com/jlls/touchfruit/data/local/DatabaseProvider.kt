package com.jlls.touchfruit.data.local

import android.content.Context
import com.jlls.touchfruit.data.local.database.TouchFruitDatabase

object DatabaseProvider {
    private var database: TouchFruitDatabase? = null

    fun init(context: Context) {
        if (database == null) {
            database = TouchFruitDatabase.getDatabase(context)
        }
    }

    fun getDatabase(): TouchFruitDatabase {
        return database ?: throw IllegalStateException("Database not initialized. Call init() first.")
    }
}
