package com.localization.offline.db

import kotlinx.coroutines.flow.Flow
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import com.localization.offline.model.FileStructure
import com.localization.offline.model.FormatSpecifier

@Entity("platform")
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

    @Query("SELECT EXISTS(SELECT 1 FROM platform where name = :platformName)")
    suspend fun isPlatformNameExist(platformName: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM platform where name = :platformName AND NOT id = :exceptPlatformId)")
    suspend fun isPlatformNameExist(platformName: String, exceptPlatformId: Int): Boolean

    @Query("UPDATE platform SET name = :name WHERE id = :platformId")
    suspend fun updateName(platformId: Int, name: String)

    @Query("UPDATE platform SET formatSpecifier = :formatSpecifier WHERE id = :platformId")
    suspend fun updateFormatSpecifier(platformId: Int, formatSpecifier: FormatSpecifier)

    @Query("UPDATE platform SET fileStructure = :fileStructure WHERE id = :platformId")
    suspend fun updateFileStructure(platformId: Int, fileStructure: FileStructure)

    @Query("UPDATE platform SET exportPrefix = :exportPrefix WHERE id = :platformId")
    suspend fun updateExportPrefix(platformId: Int, exportPrefix: String)

    @Insert
    suspend fun insert(platform: PlatformEntity)

    @Insert
    suspend fun insert(platforms: List<PlatformEntity>)

    @Query("DELETE FROM platform WHERE id = :platformId")
    suspend fun delete(platformId: Int)
}
