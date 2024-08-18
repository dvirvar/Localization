package com.localization.offline.db

import kotlinx.coroutines.flow.Flow
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import com.localization.offline.model.FileStructure
import com.localization.offline.model.FormatSpecifier

@Entity(tableName = "platform")
data class PlatformEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val fileStructure: FileStructure,
    val formatSpecifier: FormatSpecifier,
    val exportPrefix: String
)

@Dao
interface PlatformDao {
    @Query("SELECT * FROM platform")
    fun getAll(): Flow<List<PlatformEntity>>

    @Insert
    suspend fun insert(platforms: List<PlatformEntity>)
}
