package com.localization.offline.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

object DatabaseAccess {
    private val databaseName = "localization_offline.db"

    private var db: Database? = null
    var platformDao: PlatformDao? = null
    var languageDao: LanguageDao? = null
    var languageExportSettingsDao: LanguageExportSettingsDao? = null
    var customFormatSpecifierDao: CustomFormatSpecifierDao? = null
    var translationDao : TranslationDao? = null

    fun init(parent: File) {
        val dbFile = File(parent, databaseName)
        db = Room.databaseBuilder<Database>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        platformDao = db!!.platformDao()
        languageDao = db!!.languageDao()
        languageExportSettingsDao = db!!.languageExportSettingsDao()
        customFormatSpecifierDao = db!!.customFormatSpecifierDao()
        translationDao = db!!.translationDao()
    }

    fun deInit() {
        db?.close()
        db = null
        platformDao = null
        languageDao = null
        languageExportSettingsDao = null
        customFormatSpecifierDao = null
        translationDao = null
    }

    fun exists(parent: File): Boolean {
        val dbFile = File(parent, databaseName)
        return dbFile.exists()
    }
}