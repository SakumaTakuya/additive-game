package dev.seirei.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Upsert
    suspend fun upsert(session: SessionEntity)

    @Query("SELECT * FROM session WHERE slot = 1 LIMIT 1")
    suspend fun get(): SessionEntity?

    @Query("SELECT * FROM session WHERE slot = 1 LIMIT 1")
    fun observe(): Flow<SessionEntity?>

    @Query("SELECT COUNT(*) FROM session")
    suspend fun count(): Int
}

@Dao
interface ResidentDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(resident: ResidentEntity): Long

    @Query("SELECT * FROM resident ORDER BY id")
    fun observeAll(): Flow<List<ResidentEntity>>

    @Query("SELECT * FROM resident WHERE id = :id")
    suspend fun getById(id: Long): ResidentEntity?

    @Query("SELECT COUNT(*) FROM resident")
    suspend fun count(): Int
}

@Dao
interface ManaDao {
    @Upsert
    suspend fun upsert(mana: ManaEntity)

    @Query("SELECT * FROM mana WHERE id = 1")
    suspend fun get(): ManaEntity?

    @Query("SELECT * FROM mana WHERE id = 1")
    fun observe(): Flow<ManaEntity?>
}

@Dao
interface CollectionReceiptDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(receipt: CollectionReceiptEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM collection_receipt WHERE sessionId = :sessionId)")
    suspend fun exists(sessionId: Long): Boolean
}

@Dao
interface AppSettingsDao {
    @Upsert
    suspend fun upsert(settings: AppSettingsEntity)

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun get(): AppSettingsEntity?

    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observe(): Flow<AppSettingsEntity?>
}
