package com.mindurset.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Additive migrations only. Every step creates what a later version reads, so a device that
 * checked in on version 1 keeps its rows when the session, action and safety tables arrive.
 *
 * Column types and nullability must match the entities exactly, otherwise Room refuses to open the
 * database on the next launch.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `reframe_session` (" +
                "`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `checkInId` TEXT, " +
                "`currentStep` INTEGER NOT NULL, `status` TEXT NOT NULL, `promptVersion` TEXT, " +
                "`modelVersion` TEXT, `startedAt` INTEGER NOT NULL, `completedAt` INTEGER, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reframe_session_userId` ON `reframe_session` (`userId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reframe_session_createdAt` ON `reframe_session` (`createdAt`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `session_message` (" +
                "`id` TEXT NOT NULL, `sessionId` TEXT NOT NULL, `role` TEXT NOT NULL, " +
                "`content` TEXT NOT NULL, `sequence` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_message_sessionId` ON `session_message` (`sessionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_message_createdAt` ON `session_message` (`createdAt`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `session_summary` (" +
                "`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `sessionId` TEXT NOT NULL, " +
                "`summary` TEXT NOT NULL, `retentionUntil` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_summary_userId` ON `session_summary` (`userId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_summary_createdAt` ON `session_summary` (`createdAt`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `micro_action` (" +
                "`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `sessionId` TEXT, `title` TEXT NOT NULL, " +
                "`minutes` INTEGER NOT NULL, `done` INTEGER NOT NULL, `snoozedUntil` INTEGER, " +
                "`createdAt` INTEGER NOT NULL, `completedAt` INTEGER, PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_micro_action_userId` ON `micro_action` (`userId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_micro_action_createdAt` ON `micro_action` (`createdAt`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `safety_event` (" +
                "`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `level` TEXT NOT NULL, " +
                "`trigger` TEXT NOT NULL, `patternId` TEXT, `createdAt` INTEGER NOT NULL, " +
                "`retentionUntil` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_safety_event_userId` ON `safety_event` (`userId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_safety_event_createdAt` ON `safety_event` (`createdAt`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `trusted_contact` (" +
                "`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, " +
                "`relationship` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_trusted_contact_userId` ON `trusted_contact` (`userId`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `consent_record` (" +
                "`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `kind` TEXT NOT NULL, " +
                "`policyVersion` TEXT NOT NULL, `grantedAt` INTEGER NOT NULL, `revokedAt` INTEGER, " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_consent_record_userId` ON `consent_record` (`userId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_consent_record_kind` ON `consent_record` (`kind`)")
    }
}
