package com.localization.offline.service

import androidx.room.Transaction
import com.localization.offline.db.DatabaseAccess
import com.localization.offline.db.LanguageEntity
import com.localization.offline.db.LanguageExportSettingsEntity

class LanguageService {
    fun getAllLanguagesAsFlow() = DatabaseAccess.languageDao!!.getAllAsFlow()
    fun getAllLanguageExportSettingsAsFlow() = DatabaseAccess.languageExportSettingsDao!!.getAllPlatformIdToEntitiesAsFlow()
    suspend fun doesLanguageExist(name: String) = DatabaseAccess.languageDao!!.doesLanguageNameExist(name)
    suspend fun doesLanguageExist(name: String, exceptId: Int) = DatabaseAccess.languageDao!!.doesLanguageNameExist(name, exceptId)

    @Transaction
    suspend fun addLanguage(language: LanguageEntity, languageExportSettings: List<LanguageExportSettingsEntity>) {
        DatabaseAccess.languageDao!!.insert(language)
        DatabaseAccess.languageExportSettingsDao!!.insert(languageExportSettings)
    }

    suspend fun updateLanguageName(id: Int, name: String) = DatabaseAccess.languageDao!!.updateName(id, name)
    suspend fun updateLanguageOrder(language: LanguageEntity, toPriority: Int) = DatabaseAccess.languageDao!!.updateOrder(language, toPriority)
    suspend fun updateLanguageExportSettings(languageExportSettings: LanguageExportSettingsEntity) = DatabaseAccess.languageExportSettingsDao!!.update(languageExportSettings)

    suspend fun deleteLanguage(language: LanguageEntity) = DatabaseAccess.languageDao!!.deleteAndUpdateOrder(language.id, language.orderPriority)
}