package dev.seirei.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.SQLiteConstraintException

/** Raised when a collect is attempted for a session that already has a receipt. */
class DuplicateCollectionException(sessionId: Long) :
    IllegalStateException("Session $sessionId has already been collected")

/**
 * The collect transaction (design 03 §4.1): receipt → residents → mana → collected
 * flag, in a single transaction. The receipt primary key is the idempotency key —
 * a duplicate collect fails and rolls everything back, so rewards never double.
 */
@Dao
interface CollectDao {
    @Query("SELECT EXISTS(SELECT 1 FROM collection_receipt WHERE sessionId = :sessionId)")
    suspend fun receiptExists(sessionId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReceipt(receipt: CollectionReceiptEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertResidents(residents: List<ResidentEntity>)

    @Query("SELECT * FROM mana WHERE id = 1")
    suspend fun getMana(): ManaEntity?

    @Upsert
    suspend fun upsertMana(mana: ManaEntity)

    @Query("UPDATE session SET collected = 1 WHERE id = :sessionId")
    suspend fun markCollected(sessionId: Long)

    @Transaction
    suspend fun collect(sessionId: Long, awakened: List<ResidentEntity>, manaGained: Int) {
        if (receiptExists(sessionId)) throw DuplicateCollectionException(sessionId)
        try {
            insertReceipt(CollectionReceiptEntity(sessionId = sessionId))
        } catch (e: SQLiteConstraintException) {
            throw DuplicateCollectionException(sessionId)
        }
        insertResidents(awakened)
        val mana = getMana()
        upsertMana(
            ManaEntity(
                balance = (mana?.balance ?: 0) + manaGained,
                lifetimeEarned = (mana?.lifetimeEarned ?: 0) + manaGained,
            ),
        )
        markCollected(sessionId)
    }
}
