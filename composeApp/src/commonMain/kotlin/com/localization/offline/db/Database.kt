package com.localization.offline.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [
    PlatformEntity::class,
    LanguageEntity::class,
    LanguageExportSettingsEntity::class,
    CustomFormatSpecifierEntity::class,
    ],
    version = 1,
    exportSchema = false
)
abstract class Database: RoomDatabase() {
    abstract fun platformDao(): PlatformDao
    abstract fun languageDao(): LanguageDao
    abstract fun languageExportSettingsDao(): LanguageExportSettingsDao
    abstract fun customFormatSpecifierDao(): CustomFormatSpecifierDao
}