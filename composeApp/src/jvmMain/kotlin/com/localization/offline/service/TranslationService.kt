package com.localization.offline.service

import com.localization.offline.db.DatabaseAccess
import com.localization.offline.db.TranslationKeyEntity
import com.localization.offline.db.TranslationKeyPlatformEntity
import com.localization.offline.db.TranslationValueEntity

class TranslationService {
    fun getAllTranslationsAsFlow() = DatabaseAccess.translationDao!!.getAllTranslationsAsFlow()
    suspend fun getKeyPlatform(keyId: String) = DatabaseAccess.translationDao!!.getKeyPlatform(keyId)
    suspend fun doesKeyNameExist(keyName: String) = DatabaseAccess.translationDao!!.doesKeyNameExist(keyName)
    suspend fun doesKeyNameExist(keyName: String, exceptKeyId: String) = DatabaseAccess.translationDao!!.doesKeyNameExist(keyName, exceptKeyId)

    suspend fun addTranslation(key: TranslationKeyEntity, values: List<TranslationValueEntity>, platforms: List<TranslationKeyPlatformEntity>) = DatabaseAccess.translationDao!!.insertTranslation(key, values, platforms)

    suspend fun updateTranslationKey(keyId: String, key: String, description: String, insertKeyPlatform: List<TranslationKeyPlatformEntity>, deleteKeyPlatform: List<TranslationKeyPlatformEntity>) = DatabaseAccess.translationDao!!.updateTranslationKey(keyId, key, description, insertKeyPlatform, deleteKeyPlatform)
    suspend fun updateTranslation(value: TranslationValueEntity) = DatabaseAccess.translationDao!!.upsertValue(value)
    suspend fun updateTranslations(values: List<TranslationValueEntity>) = DatabaseAccess.translationDao!!.upsertValues(values)

    suspend fun deleteTranslation(keyId: String) {
        DatabaseAccess.translationDao!!.deleteTranslation(keyId)
    }
}