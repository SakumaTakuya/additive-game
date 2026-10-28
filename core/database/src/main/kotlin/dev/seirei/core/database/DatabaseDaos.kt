package dev.seirei.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import dev.seirei.core.model.normalizeNickname
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
abstract class ResidentDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(resident: ResidentEntity): Long

    @Query("SELECT * FROM resident ORDER BY id")
    abstract fun observeAll(): Flow<List<ResidentEntity>>

    @Query("SELECT * FROM resident WHERE id = :id")
    abstract suspend fun getById(id: Long): ResidentEntity?

    @Query("SELECT COUNT(*) FROM resident")
    abstract suspend fun count(): Int

    @Query("UPDATE resident SET nickname = :nickname WHERE id = :id")
    abstract suspend fun updateNicknameRow(id: Long, nickname: String?)

    /**
     * ニックネームの単一列更新（設計書03 §4.5）。改行・制御文字を取り除き、
     * モデルの正規化（前後空白の除去・空文字→NULL・最大10文字）を適用してから書く。
     */
    open suspend fun updateNickname(id: Long, nickname: String?) {
        val withoutControls = nickname?.filterNot { it.isISOControl() }
        updateNicknameRow(id, normalizeNickname(withoutControls))
    }

    /** 保護（locked）の切り替え（設計書03 §4.5）。 */
    @Query("UPDATE resident SET locked = :locked WHERE id = :id")
    abstract suspend fun updateLocked(id: Long, locked: Boolean)
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
