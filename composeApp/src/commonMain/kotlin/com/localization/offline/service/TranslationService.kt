package com.localization.offline.service

import com.localization.offline.db.DatabaseAccess
import com.localization.offline.db.TranslationKeyEntity
import com.localization.offline.db.TranslationKeyPlatformEntity
import com.localization.offline.db.TranslationValueEntity

class TranslationService {

    fun getAllKeys() = DatabaseAccess.translationDao!!.getAllKeysAsFlow()
    fun getAllValues() = DatabaseAccess.translationDao!!.getAllValuesAsFlow()
    suspend fun getKeyPlatform(keyId: String) = DatabaseAccess.translationDao!!.getKeyPlatform(keyId)
    suspend fun isKeyNameExist(keyName: String) = DatabaseAccess.translationDao!!.isKeyNameExist(keyName)
    suspend fun isKeyNameExist(keyName: String, exceptKeyId: String) = DatabaseAccess.translationDao!!.isKeyNameExist(keyName, exceptKeyId)

    suspend fun addTranslation(key: TranslationKeyEntity, values: List<TranslationValueEntity>, platforms: List<TranslationKeyPlatformEntity>) {
        DatabaseAccess.translationDao!!.insertTranslation(key, values, platforms)
    }

    suspend fun updateTranslationKey(id: String, key: String, description: String) {
        DatabaseAccess.translationDao!!.updateKey(id, key, description)
    }

    suspend fun updateTranslationKey(keyId: String, key: String, description: String, insertKeyPlatform: List<TranslationKeyPlatformEntity>, deleteKeyPlatform: List<TranslationKeyPlatformEntity>) {
        DatabaseAccess.translationDao!!.updateTranslationKey(keyId, key, description, insertKeyPlatform, deleteKeyPlatform)
    }

    suspend fun updateTranslation(value: TranslationValueEntity) {
        DatabaseAccess.translationDao!!.upsertValue(value)
    }

    suspend fun deleteTranslation(keyId: String) {
        DatabaseAccess.translationDao!!.deleteTranslation(keyId)
    }
}