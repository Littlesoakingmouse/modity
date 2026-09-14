package com.example.modity.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ModityDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION,
) {
    override fun onCreate(database: SQLiteDatabase) {
        // Tables will be introduced with their features so migrations stay explicit.
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Add versioned migrations here when the first schema is introduced.
    }

    private companion object {
        const val DATABASE_NAME = "modity.db"
        const val DATABASE_VERSION = 1
    }
}
