package com.mindurset.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserAccountEntity::class,
        UserProfileEntity::class,
        CheckInLogEntity::class,
        JournalEntryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class MindUrSetDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao

    abstract fun profileDao(): ProfileDao

    abstract fun checkInDao(): CheckInDao

    abstract fun journalDao(): JournalDao

    companion object {
        const val NAME = "mindurset.db"

        fun build(context: Context): MindUrSetDatabase =
            Room.databaseBuilder(context.applicationContext, MindUrSetDatabase::class.java, NAME).build()
    }
}
