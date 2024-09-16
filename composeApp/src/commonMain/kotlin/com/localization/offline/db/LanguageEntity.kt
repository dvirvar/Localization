package com.localization.offline.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity("language",
    [Index("orderPriority")])
data class LanguageEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val orderPriority: Int
)

@Dao
interface LanguageDao {
    @Query("SELECT * FROM language ORDER BY orderPriority")
    fun getAllAsFlow(): Flow<List<LanguageEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM language where name = :languageName)")
    suspend fun isLanguageNameExist(languageName: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM language where name = :languageName AND NOT id = :exceptLanguageId)")
    suspend fun isLanguageNameExist(languageName: String, exceptLanguageId: Int): Boolean

    @Query("UPDATE language SET name = :name WHERE id = :languageId")
    suspend fun updateName(languageId: Int, name: String)

    @Query("UPDATE language SET orderPriority = :orderPriority WHERE id = :languageId")
    suspend fun updateOrderPriority(languageId: Int, orderPriority: Int)

    @Query("UPDATE language SET orderPriority = orderPriority + :incrementValue WHERE orderPriority >= :fromPriority AND orderPriority < :toPriority")
    suspend fun updateOrderOfAllBetween(fromPriority: Int, toPriority: Int, incrementValue: Int)

    @Query("UPDATE language SET orderPriority = orderPriority + :incrementValue WHERE orderPriority > :orderPriority")
    suspend fun updateOrderOfAllAfter(orderPriority: Int, incrementValue: Int)

    @Transaction
    suspend fun updateOrder(language: LanguageEntity, toPriority: Int) {
        if (language.orderPriority > toPriority) {
            updateOrderOfAllBetween(toPriority, language.orderPriority, 1)
        } else {
            updateOrderOfAllBetween(language.orderPriority + 1, toPriority + 1, -1)
        }
        updateOrderPriority(language.id, toPriority)
    }

    @Update
    suspend fun update(language: List<LanguageEntity>)

    @Insert
    suspend fun insert(language: LanguageEntity)

    @Insert
    suspend fun insert(languages: List<LanguageEntity>)

    @Transaction
    suspend fun deleteAndUpdateOrder(languageId: Int, orderPriority: Int) {
        delete(languageId)
        updateOrderOfAllAfter(orderPriority, -1)
    }

    @Query("DELETE FROM language WHERE id = :languageId")
    suspend fun delete(languageId: Int)
}
