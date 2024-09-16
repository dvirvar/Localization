package com.localization.offline.service

import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.localization.offline.db.DatabaseAccess
import com.localization.offline.db.LanguageEntity
import com.localization.offline.db.PlatformEntity
import com.localization.offline.db.TranslationKeyEntity
import com.localization.offline.db.TranslationKeyPlatformEntity
import com.localization.offline.db.TranslationValueEntity
import com.localization.offline.model.FileStructure
import com.localization.offline.model.FileStructureBuilderFactory
import com.localization.offline.model.FormatSpecifier
import com.localization.offline.model.FormatSpecifierFormatterFactory
import java.io.File
import java.util.UUID

class ImportService {
    suspend fun import(fileStructure: FileStructure, formatSpecifier: FormatSpecifier, languagePaths: List<Pair<LanguageEntity, String>>, platforms: List<PlatformEntity>) {
        val fileStructureBuilder = FileStructureBuilderFactory.getBy(fileStructure)
        val formatSpecifierFormatter = FormatSpecifierFormatterFactory.getBy(when(formatSpecifier) {
            FormatSpecifier.Java, FormatSpecifier.AppleEcosystem,
            FormatSpecifier.I18n, FormatSpecifier.None -> FormatSpecifierFormatterFactory.Argument.Empty(formatSpecifier)
            FormatSpecifier.Custom -> FormatSpecifierFormatterFactory.Argument.Custom(listOf())
        })
        val databasePlatforms = DatabaseAccess.platformDao!!.getAll()
        languagePaths.fastForEach { languagePath ->
            val language = languagePath.first
            val path = languagePath.second
            try {
                val fileBody = File(path).inputStream().use { stream ->
                    stream.readAllBytes().decodeToString()
                }
                val keyValues = fileStructureBuilder.deconstruct(fileBody).toMutableList()
                formatSpecifierFormatter?.let {
                    for (index in keyValues.indices) {
                        keyValues[index] = keyValues[index].copy(second = formatSpecifierFormatter.toAppFormat(keyValues[index].second))
                    }
                }
                keyValues.fastForEach { keyValue ->
                    val key = keyValue.first
                    val value = keyValue.second
                    val keyId = DatabaseAccess.translationDao!!.getKeyId(key)
                    if (keyId == null) {
                        val keyEntityId = UUID.randomUUID().toString()
                        DatabaseAccess.translationDao!!.insertTranslation(
                            TranslationKeyEntity(keyEntityId, key, ""),
                            TranslationValueEntity(keyEntityId, language.id, value),
                            platforms.fastMap { TranslationKeyPlatformEntity(keyEntityId, it.id) }
                        )
                    } else {
                        val keyPlatformEntities = DatabaseAccess.translationDao!!.getKeyPlatform(keyId)
                        val insertKeyPlatform = mutableListOf<TranslationKeyPlatformEntity>()
                        val deleteKeyPlatform = mutableListOf<TranslationKeyPlatformEntity>()
                        databasePlatforms.fastForEach { platform ->
                            val wasSelected = keyPlatformEntities.fastAny { it.platformId == platform.id }
                            val isSelected = platforms.fastAny { it.id == platform.id }
                            if (!wasSelected && isSelected) {
                                insertKeyPlatform.add(TranslationKeyPlatformEntity(keyId, platform.id))
                            } else if (wasSelected && !isSelected) {
                                deleteKeyPlatform.add(TranslationKeyPlatformEntity(keyId, platform.id))
                            }
                        }
                        DatabaseAccess.translationDao!!.upsertValue(TranslationValueEntity(keyId, language.id, value))
                        DatabaseAccess.translationDao!!.insertKeyPlatform(insertKeyPlatform)
                        DatabaseAccess.translationDao!!.deleteKeyPlatform(deleteKeyPlatform)
                    }
                }
            } catch (e: Exception) {
                println("Error while importing: $e")
            }
        }
    }
}