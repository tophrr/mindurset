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
        ReframeSessionEntity::class,
        SessionMessageEntity::class,
        SessionSummaryEntity::class,
        MicroActionEntity::class,
        SafetyEventEntity::class,
        TrustedContactEntity::class,
        ConsentRecordEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class MindUrSetDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao

    abstract fun profileDao(): ProfileDao

    abstract fun checkInDao(): CheckInDao

    abstract fun journalDao(): JournalDao

    abstract fun reframeSessionDao(): ReframeSessionDao

    abstract fun sessionMessageDao(): SessionMessageDao

    abstract fun sessionSummaryDao(): SessionSummaryDao

    abstract fun microActionDao(): MicroActionDao

    abstract fun safetyEventDao(): SafetyEventDao

    abstract fun trustedContactDao(): TrustedContactDao

    abstract fun consentRecordDao(): ConsentRecordDao

    companion object {
        const val NAME = "mindurset.db"

        fun build(context: Context): MindUrSetDatabase =
            Room.databaseBuilder(context.applicationContext, MindUrSetDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
