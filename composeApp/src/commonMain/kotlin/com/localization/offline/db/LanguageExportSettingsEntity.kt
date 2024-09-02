package com.localization.offline.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity("language_export_settings",
    [Index("languageId", "platformId", unique = true)],
    primaryKeys = ["languageId", "platformId"],
    foreignKeys = [
        ForeignKey(LanguageEntity::class, ["id"], ["languageId"], ForeignKey.CASCADE),
        ForeignKey(PlatformEntity::class, ["id"], ["platformId"], ForeignKey.CASCADE)
    ])
data class LanguageExportSettingsEntity(
    val languageId: Int,
    val platformId: Int,
    val folderSuffix: String,
    val fileName: String,
)

@Dao
interface LanguageExportSettingsDao {
    @Query("SELECT * FROM language_export_settings")
    fun getAll(): Flow<List<LanguageExportSettingsEntity>>

    @Insert
    suspend fun insert(languageExportSettings: List<LanguageExportSettingsEntity>)
}
