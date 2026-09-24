package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RemoteDeviceEntity::class, SessionLogEntity::class], version = 1, exportSchema = false)
abstract class DeskFlowDatabase : RoomDatabase() {
    abstract fun deskFlowDao(): DeskFlowDao

    companion object {
        @Volatile
        private var INSTANCE: DeskFlowDatabase? = null

        fun getDatabase(context: Context): DeskFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DeskFlowDatabase::class.java,
                    "deskflow_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
