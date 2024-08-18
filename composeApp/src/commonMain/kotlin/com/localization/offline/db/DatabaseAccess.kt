package com.localization.offline.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

object DatabaseAccess {
    private val databaseName = "localization_offline.db"

    private var db: Database? = null
    val platformDao by lazy {db!!.platformDao()}
    val languageDao by lazy {db!!.languageDao()}
    val languageExportSettingsDao by lazy {db!!.languageExportSettingsDao()}
    val customFormatSpecifierDao by lazy {db!!.customFormatSpecifierDao()}

    fun init(parent: File) {
        val dbFile = File(parent, databaseName)
        db = Room.databaseBuilder<Database>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    fun deInit() {
        db?.close()
        db = null
    }

    fun exists(parent: File): Boolean {
        val dbFile = File(parent, databaseName)
        return dbFile.exists()
    }
}