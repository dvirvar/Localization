package com.localization.offline.db

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.MapColumn
import androidx.room3.Query
import androidx.room3.RewriteQueriesToDropUnusedColumns
import androidx.room3.Update
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
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM language_export_settings AS les " +
            "JOIN language AS l " +
            "ON les.languageId = l.id " +
            "ORDER BY l.orderPriority")
    fun getAllPlatformIdToEntitiesAsFlow(): Flow<Map<@MapColumn("platformId") Int, List<LanguageExportSettingsEntity>>>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM language_export_settings AS les " +
            "JOIN language AS l " +
            "ON les.languageId = l.id " +
            "ORDER BY l.orderPriority")
    suspend fun getAllPlatformIdToEntities(): Map<@MapColumn("platformId") Int, List<LanguageExportSettingsEntity>>

    @Insert
    suspend fun insert(languageExportSettings: List<LanguageExportSettingsEntity>)

    @Update
    suspend fun update(languageExportSettings: LanguageExportSettingsEntity)
}
