package com.localization.offline.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.MapColumn
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
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
