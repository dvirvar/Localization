package com.localization.offline.db

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.MapColumn
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Entity("custom_format_specifier",
    [Index("platformId")],
    foreignKeys = [ForeignKey(PlatformEntity::class, ["id"], ["platformId"], ForeignKey.CASCADE)])
data class CustomFormatSpecifierEntity(
    @PrimaryKey val id: Int,
    val platformId: Int,
    val from: String,
    val to: String
)

@Dao
interface CustomFormatSpecifierDao {
    @Query("SELECT * FROM custom_format_specifier")
    fun getAllPlatformIdToEntitiesAsFlow(): Flow<Map<@MapColumn("platformId") Int, List<CustomFormatSpecifierEntity>>>

    @Query("SELECT * FROM custom_format_specifier " +
            "WHERE platformId = :platformId")
    suspend fun getAll(platformId: Int): List<CustomFormatSpecifierEntity>

    @Insert
    suspend fun insert(customFormatSpecifiers: List<CustomFormatSpecifierEntity>)

    @Update
    suspend fun update(customFormatSpecifier: CustomFormatSpecifierEntity)

    @Query("DELETE FROM custom_format_specifier WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM custom_format_specifier WHERE platformId = :platformId")
    suspend fun deleteAllOfPlatform(platformId: Int)
}
