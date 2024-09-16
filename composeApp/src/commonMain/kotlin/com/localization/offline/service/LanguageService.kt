package com.localization.offline.service

import androidx.room.Transaction
import com.localization.offline.db.DatabaseAccess
import com.localization.offline.db.LanguageEntity
import com.localization.offline.db.LanguageExportSettingsEntity

class LanguageService {

    fun getAllLanguages() = DatabaseAccess.languageDao!!.getAllAsFlow()
    fun getAllLanguageExportSettings() = DatabaseAccess.languageExportSettingsDao!!.getAllAsFlow()
    suspend fun isLanguageExist(name: String) = DatabaseAccess.languageDao!!.isLanguageNameExist(name)
    suspend fun isLanguageExist(name: String, exceptId: Int) = DatabaseAccess.languageDao!!.isLanguageNameExist(name, exceptId)

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