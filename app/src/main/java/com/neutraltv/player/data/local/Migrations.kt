package com.neutraltv.player.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add new columns to channels
        db.execSQL("ALTER TABLE channels ADD COLUMN lastWatchedAt INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE channels ADD COLUMN epgChannelId TEXT DEFAULT NULL")

        // Add new column to playlists
        db.execSQL("ALTER TABLE playlists ADD COLUMN epgUrl TEXT DEFAULT NULL")

        // Create favorites table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS favorites (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                channelId INTEGER NOT NULL,
                addedAt INTEGER NOT NULL,
                FOREIGN KEY (channelId) REFERENCES channels(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_favorites_channelId ON favorites(channelId)")

        // Create programs table (EPG)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS programs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                epgChannelId TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT,
                startTime INTEGER NOT NULL,
                endTime INTEGER NOT NULL,
                category TEXT,
                playlistId INTEGER NOT NULL,
                FOREIGN KEY (playlistId) REFERENCES playlists(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_programs_epgChannelId ON programs(epgChannelId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_programs_playlistId ON programs(playlistId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_programs_startTime ON programs(startTime)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE channels ADD COLUMN channelType TEXT NOT NULL DEFAULT 'live'")
        db.execSQL("ALTER TABLE channels ADD COLUMN vodProgress INTEGER NOT NULL DEFAULT 0")
    }
}
