package dev.seirei.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

/**
 * 指名覚醒の書き込み（設計書03 §4.3）。住人の追加と魔力の減算を1トランザクションにし、
 * 残高とモチーフの発見状態を書き込み時点でも再チェックする。再チェックに落ちたら
 * 何も書かず false を返す。
 *
 * 発見済みモチーフは「所持したことのある住人のモチーフ」で判定する。v3 スキーマには
 * 図鑑専用のテーブルがないため、現在の住人一覧の distinct なモチーフが唯一の根拠になる。
 */
@Dao
abstract class NamedAwakenDao {
    @Query("SELECT * FROM mana WHERE id = 1")
    abstract suspend fun mana(): ManaEntity?

    @Query("SELECT DISTINCT motifId FROM resident")
    abstract suspend fun discoveredMotifIds(): List<Int>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertResident(resident: ResidentEntity): Long

    @Upsert
    abstract suspend fun upsertMana(mana: ManaEntity)

    @Transaction
    open suspend fun awaken(resident: ResidentEntity, cost: Int): Boolean {
        val mana = mana()
        val balance = mana?.balance ?: 0
        if (balance < cost) return false
        if (resident.motifId !in discoveredMotifIds()) return false

        insertResident(resident)
        upsertMana(
            ManaEntity(
                balance = balance - cost,
                lifetimeEarned = mana?.lifetimeEarned ?: 0,
            ),
        )
        return true
    }
}
