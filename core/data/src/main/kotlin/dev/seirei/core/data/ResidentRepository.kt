package dev.seirei.core.data

import dev.seirei.core.database.SeireiDatabase

/**
 * 住人の単一列更新（設計書03 §4.5）。ニックネームの正規化は ResidentDao 側で行う。
 * UI 上の呼称は「保護」（DEC-10）。
 */
class ResidentRepository(
    private val database: SeireiDatabase,
) {
    suspend fun updateNickname(residentId: Long, nickname: String?) {
        database.residentDao().updateNickname(residentId, nickname)
    }

    suspend fun setLocked(residentId: Long, locked: Boolean) {
        database.residentDao().updateLocked(residentId, locked)
    }
}
