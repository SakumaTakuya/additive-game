package dev.seirei.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/**
 * 初期付与の書き込み（設計書03 §4.6）。判定（住人0体かつセッション行なし）と
 * 挿入を1トランザクションにし、判定後に状態が変わっていたら何も書かず false を返す。
 * 2回目以降の呼び出しは先行する住人を見て false になるため冪等。
 */
@Dao
abstract class SeedInitialDao {
    @Query("SELECT COUNT(*) FROM resident")
    abstract suspend fun countResidents(): Int

    @Query("SELECT COUNT(*) FROM session")
    abstract suspend fun countSessions(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertResident(resident: ResidentEntity): Long

    @Transaction
    open suspend fun insertInitial(resident: ResidentEntity): Boolean {
        if (countResidents() > 0 || countSessions() > 0) return false
        insertResident(resident)
        return true
    }
}
