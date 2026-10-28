package dev.seirei.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

/**
 * 点火の書き込み（設計書03 §4.2）。単一行の置換を、読み取った現在行と一致する
 * ときだけ行う（compare-and-replace）。読み取り後に別操作が割り込んでいたら
 * 何も書かず false を返す。
 */
@Dao
abstract class SessionWriteDao {
    @Query("SELECT * FROM session WHERE slot = 1 LIMIT 1")
    abstract suspend fun current(): SessionEntity?

    @Upsert
    abstract suspend fun upsert(session: SessionEntity)

    @Transaction
    open suspend fun replaceIfUnchanged(
        expected: SessionEntity?,
        replacement: SessionEntity,
    ): Boolean {
        if (current() != expected) return false
        upsert(replacement)
        return true
    }
}
