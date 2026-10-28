package dev.seirei.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

/** 還元対象1件：住人IDと、その個体について呼び出し側が計算した還元量。 */
data class ReturnTargetRow(val residentId: Long, val refund: Int)

/**
 * 魔力に還す書き込み（設計書03 §4.4）。削除と加算を1トランザクションにし、
 * 対象外（locked・編成中）を書き込み時点の永続状態でも再チェックする。
 * 再チェックで外れた個体は削除されず、還元量にも数えない。
 */
@Dao
abstract class ReturnToManaDao {
    @Query("SELECT * FROM resident WHERE id = :id")
    abstract suspend fun loadById(id: Long): ResidentEntity?

    @Query("SELECT * FROM session WHERE slot = 1 LIMIT 1")
    abstract suspend fun currentSession(): SessionEntity?

    @Query("DELETE FROM resident WHERE id = :id AND locked = 0")
    abstract suspend fun deleteIfUnlocked(id: Long): Int

    @Query("SELECT * FROM mana WHERE id = 1")
    abstract suspend fun mana(): ManaEntity?

    @Upsert
    abstract suspend fun upsertMana(mana: ManaEntity)

    /** 実際に削除できた個体の還元量の合計を返す。1件も消えなければ 0。 */
    @Transaction
    open suspend fun returnToMana(targets: List<ReturnTargetRow>): Int {
        val partyIds = currentSession()?.partyIds?.toSet().orEmpty()
        var refunded = 0
        for (target in targets) {
            val row = loadById(target.residentId) ?: continue
            if (row.locked || row.id in partyIds) continue
            if (deleteIfUnlocked(row.id) == 1) {
                refunded += target.refund
            }
        }
        if (refunded > 0) {
            val mana = mana()
            upsertMana(
                ManaEntity(
                    balance = (mana?.balance ?: 0) + refunded,
                    lifetimeEarned = (mana?.lifetimeEarned ?: 0) + refunded,
                ),
            )
        }
        return refunded
    }
}
