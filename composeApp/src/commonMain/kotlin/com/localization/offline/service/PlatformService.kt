package com.localization.offline.service

import androidx.room.Transaction
import com.localization.offline.db.CustomFormatSpecifierEntity
import com.localization.offline.db.DatabaseAccess
import com.localization.offline.db.PlatformEntity
import com.localization.offline.model.EmptyTranslationExport
import com.localization.offline.model.FileStructure
import com.localization.offline.model.FormatSpecifier

class PlatformService {

    fun getAllPlatforms() = DatabaseAccess.platformDao!!.getAllAsFlow()
    fun getAllCustomFormatSpecifiers() = DatabaseAccess.customFormatSpecifierDao!!.getAllAsFlow()
    suspend fun isPlatformExist(name: String) = DatabaseAccess.platformDao!!.isPlatformNameExist(name)
    suspend fun isPlatformExist(name: String, exceptId: Int) = DatabaseAccess.platformDao!!.isPlatformNameExist(name, exceptId)

    @Transaction
    suspend fun addPlatform(platform: PlatformEntity, customFormatSpecifiers: List<CustomFormatSpecifierEntity>) {
        DatabaseAccess.platformDao!!.insert(platform)
        DatabaseAccess.customFormatSpecifierDao!!.insert(customFormatSpecifiers)
    }
    suspend fun addCustomFormatSpecifiers(customFormatSpecifiers: List<CustomFormatSpecifierEntity>) = DatabaseAccess.customFormatSpecifierDao!!.insert(customFormatSpecifiers)

    suspend fun updatePlatformName(id: Int, name: String) = DatabaseAccess.platformDao!!.updateName(id, name)
    suspend fun updatePlatformFormatSpecifier(platformId: Int, formatSpecifier: FormatSpecifier) = DatabaseAccess.platformDao!!.updateFormatSpecifier(platformId, formatSpecifier)
    suspend fun updatePlatformEmptyTranslationExport(platformId: Int, emptyTranslationExport: EmptyTranslationExport) = DatabaseAccess.platformDao!!.updateEmptyTranslationExport(platformId, emptyTranslationExport)
    suspend fun updatePlatformFileStructure(platformId: Int, fileStructure: FileStructure) = DatabaseAccess.platformDao!!.updateFileStructure(platformId, fileStructure)
    suspend fun updatePlatformExportPrefix(platformId: Int, exportPrefix: String) = DatabaseAccess.platformDao!!.updateExportPrefix(platformId, exportPrefix)
    suspend fun updatePlatformExportToPath(platformId: Int, exportToPath: String) = DatabaseAccess.platformDao!!.updateExportToPath(platformId, exportToPath)
    suspend fun updateCustomFormatSpecifier(customFormatSpecifier: CustomFormatSpecifierEntity) = DatabaseAccess.customFormatSpecifierDao!!.update(customFormatSpecifier)

    @Transaction
    suspend fun deletePlatform(platform: PlatformEntity) {
        DatabaseAccess.platformDao!!.delete(platform.id)
        deletePlatformCustomFormatSpecifiers(platform.id)
    }

    suspend fun deleteCustomFormatSpecifier(customFormatSpecifierId: Int) = DatabaseAccess.customFormatSpecifierDao!!.delete(customFormatSpecifierId)
    suspend fun deletePlatformCustomFormatSpecifiers(platformId: Int) = DatabaseAccess.customFormatSpecifierDao!!.deleteAllOfPlatform(platformId)
}