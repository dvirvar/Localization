package com.localization.offline.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Update
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
    suspend fun getAll(): Map<@MapColumn("platformId") Int, List<LanguageExportSettingsEntity>>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM language_export_settings AS les " +
            "JOIN language AS l " +
            "ON les.languageId = l.id " +
            "ORDER BY l.orderPriority")
    fun getAllAsFlow(): Flow<Map<@MapColumn("platformId") Int, List<LanguageExportSettingsEntity>>>

    @Update
    suspend fun update(languageExportSettings: LanguageExportSettingsEntity)

    @Insert
    suspend fun insert(languageExportSettings: List<LanguageExportSettingsEntity>)
}
