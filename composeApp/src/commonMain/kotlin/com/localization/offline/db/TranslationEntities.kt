package com.localization.offline.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity("translation_key",
    [Index("key", unique = true)]
    )
data class TranslationKeyEntity(
    @PrimaryKey val id: String,
    val key: String,
    val description: String
)

@Entity("translation_value",
    [Index("keyId", "languageId", unique = true)],
    primaryKeys = ["keyId", "languageId"],
    foreignKeys = [
        ForeignKey(TranslationKeyEntity::class, ["id"], ["keyId"], ForeignKey.CASCADE),
        ForeignKey(LanguageEntity::class, ["id"], ["languageId"], ForeignKey.CASCADE),
    ])
data class TranslationValueEntity(
    val keyId: String,
    val languageId: Int,
    val value: String
)

@Entity("translation_key_platform",
    [Index("platformId", "keyId", unique = true)],
    primaryKeys = ["keyId", "platformId"],
    foreignKeys = [
        ForeignKey(TranslationKeyEntity::class, ["id"], ["keyId"], ForeignKey.CASCADE),
        ForeignKey(PlatformEntity::class, ["id"], ["platformId"], ForeignKey.CASCADE)
    ])
data class TranslationKeyPlatformEntity(
    val keyId: String,
    val platformId: Int
)

data class TranslationKeyWithValues(
    @Embedded val key: TranslationKeyEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "keyId"
    )
    val values: List<TranslationValueEntity>
)

@Dao
interface TranslationDao {
    data class KeyValue(
        val key: String,
        val value: String
    )

    @Transaction
    @Query("SELECT * FROM translation_key")
    fun getAllTranslationsAsFlow(): Flow<List<TranslationKeyWithValues>>

    @Query("SELECT k.`key`, v.value FROM translation_key AS k " +
            "INNER JOIN translation_value AS v " +
            "INNER JOIN translation_key_platform AS kp " +
            "ON k.id = v.keyId " +
            "AND k.id = kp.keyId " +
            "WHERE kp.platformId = :platformId " +
            "AND v.languageId = :languageId")
    suspend fun getAllKeyValue(platformId: Int, languageId: Int): List<KeyValue>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM translation_key AS k " +
            "INNER JOIN translation_key_platform AS kp " +
            "ON k.id = kp.keyId " +
            "AND kp.platformId = :platformId " +
            "WHERE NOT EXISTS( " +
            "SELECT 1 FROM translation_value AS v " +
            "WHERE k.id = v.keyId " +
            "AND kp.platformId = :platformId " +
            "AND v.languageId = :languageId " +
            ")")
    suspend fun getAllKeys(platformId: Int, languageId: Int): List<TranslationKeyEntity>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM translation_key AS k " +
            "INNER JOIN translation_value AS v " +
            "ON k.id = v.keyId " +
            "WHERE v.languageId IN (:languageIds)")
    suspend fun getAllKeyToValues(languageIds: List<Int>): Map<TranslationKeyEntity, List<TranslationValueEntity>>

    @Query("SELECT id FROM translation_key")
    suspend fun getAllKeyIds(): List<String>

    @Query("SELECT id FROM translation_key WHERE `key` = :keyName")
    suspend fun getKeyId(keyName: String): String?

    @Query("SELECT * FROM translation_key_platform WHERE keyId = :keyId")
    suspend fun getKeyPlatform(keyId: String): List<TranslationKeyPlatformEntity>

    @Query("SELECT value FROM translation_value AS v " +
            "INNER JOIN language AS l " +
            "ON v.languageId == l.id " +
            "WHERE v.keyId = :keyId " +
            "AND l.orderPriority = 0")
    suspend fun getBaseLanguage(keyId: String): String?

    @Query("SELECT EXISTS(SELECT 1 FROM translation_key where `key` = :keyName)")
    suspend fun doesKeyNameExist(keyName: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM translation_key where `key` = :keyName AND NOT id = :exceptKeyId)")
    suspend fun doesKeyNameExist(keyName: String, exceptKeyId: String): Boolean

    @Transaction
    suspend fun insertTranslation(key: TranslationKeyEntity, value: TranslationValueEntity, platforms: List<TranslationKeyPlatformEntity>) {
        insertKey(key)
        insertValue(value)
        insertKeyPlatform(platforms)
    }

    @Transaction
    suspend fun insertTranslation(key: TranslationKeyEntity, values: List<TranslationValueEntity>, platforms: List<TranslationKeyPlatformEntity>) {
        insertKey(key)
        insertValues(values)
        insertKeyPlatform(platforms)
    }

    @Insert
    suspend fun insertKey(key: TranslationKeyEntity)

    @Insert
    suspend fun insertValue(value: TranslationValueEntity)

    @Insert
    suspend fun insertValues(values: List<TranslationValueEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertKeyPlatform(keyPlatform: List<TranslationKeyPlatformEntity>)

    @Upsert
    suspend fun upsertValue(value: TranslationValueEntity)

    @Upsert
    suspend fun upsertValues(values: List<TranslationValueEntity>)

    @Transaction
    suspend fun updateTranslationKey(keyId: String, key: String, description: String, insertKeyPlatform: List<TranslationKeyPlatformEntity>, deleteKeyPlatform: List<TranslationKeyPlatformEntity>) {
        updateKey(keyId, key, description)
        insertKeyPlatform(insertKeyPlatform)
        deleteKeyPlatform(deleteKeyPlatform)
    }

    @Transaction
    suspend fun upsertTranslationValue(keyId: String, languageId: Int, value: String, insertKeyPlatform: List<TranslationKeyPlatformEntity>, deleteKeyPlatform: List<TranslationKeyPlatformEntity>) {
        upsertValue(TranslationValueEntity(keyId, languageId, value))
        insertKeyPlatform(insertKeyPlatform)
        deleteKeyPlatform(deleteKeyPlatform)
    }

    @Query("UPDATE translation_key SET `key` = :key, description = :description WHERE id = :id")
    suspend fun updateKey(id: String, key: String, description: String)

    @Query("DELETE FROM translation_key WHERE id = :keyId")
    suspend fun deleteTranslation(keyId: String)

    @Delete
    suspend fun deleteKeyPlatform(keyPlatform: List<TranslationKeyPlatformEntity>)
}