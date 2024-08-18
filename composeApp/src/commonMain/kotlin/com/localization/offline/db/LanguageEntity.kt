package com.localization.offline.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "language")
data class LanguageEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val orderPriority: Int
)

@Dao
interface LanguageDao {
    @Query("SELECT * FROM language")
    fun getAll(): Flow<List<LanguageEntity>>

    @Insert
    suspend fun insert(languages: List<LanguageEntity>)
}
