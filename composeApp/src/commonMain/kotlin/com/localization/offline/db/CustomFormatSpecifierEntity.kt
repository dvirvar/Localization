package com.localization.offline.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
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
    fun getAll(): Flow<List<CustomFormatSpecifierEntity>>

    @Insert
    suspend fun insert(customFormatSpecifiers: List<CustomFormatSpecifierEntity>)
}
