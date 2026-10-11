package com.example.modity.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.modity.model.FocusSession
import com.example.modity.model.Task
import com.example.modity.model.TaskGroup
import com.example.modity.model.TaskOccurrence

@Database(
    entities = [TaskGroup::class, Task::class, TaskOccurrence::class, FocusSession::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskGroupDao(): TaskGroupDao
    abstract fun taskDao(): TaskDao
    abstract fun occurrenceDao(): TaskOccurrenceDao
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "modity.db")
                // Schema is still evolving during development; old local data is discarded on upgrade.
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    }
}
