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

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add Xtream fields to playlists
        db.execSQL("ALTER TABLE playlists ADD COLUMN type TEXT NOT NULL DEFAULT 'm3u'")
        db.execSQL("ALTER TABLE playlists ADD COLUMN serverUrl TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE playlists ADD COLUMN username TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE playlists ADD COLUMN password TEXT DEFAULT NULL")

        // Add Xtream fields to channels
        db.execSQL("ALTER TABLE channels ADD COLUMN streamId INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE channels ADD COLUMN categoryId TEXT DEFAULT NULL")

        // Create series table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS series (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                playlistId INTEGER NOT NULL,
                seriesId INTEGER NOT NULL,
                name TEXT NOT NULL,
                cover TEXT,
                categoryName TEXT,
                rating TEXT,
                plot TEXT,
                FOREIGN KEY (playlistId) REFERENCES playlists(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_series_playlistId ON series(playlistId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_series_seriesId ON series(seriesId)")

        // Create episodes table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS episodes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                seriesEntityId INTEGER NOT NULL,
                playlistId INTEGER NOT NULL,
                season INTEGER NOT NULL,
                episodeNum INTEGER NOT NULL,
                title TEXT NOT NULL,
                streamUrl TEXT NOT NULL,
                containerExtension TEXT,
                duration TEXT,
                plot TEXT,
                progress INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (seriesEntityId) REFERENCES series(id) ON DELETE CASCADE,
                FOREIGN KEY (playlistId) REFERENCES playlists(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_episodes_seriesEntityId ON episodes(seriesEntityId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_episodes_playlistId ON episodes(playlistId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_episodes_season ON episodes(season)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Composite index for EPG time-range queries
        db.execSQL("CREATE INDEX IF NOT EXISTS index_programs_epgChannelId_startTime_endTime ON programs(epgChannelId, startTime, endTime)")
        // Composite index for channel list queries
        db.execSQL("CREATE INDEX IF NOT EXISTS index_channels_playlistId_channelType_isHidden ON channels(playlistId, channelType, isHidden)")
    }
}
