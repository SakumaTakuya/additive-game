package dev.seirei.core.data

import dev.seirei.core.database.SeireiDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** 魔力の残高と累計獲得（設計書03 §5）。天井ゲージ表示の入力。 */
data class ManaBalance(
    val balance: Int,
    val lifetimeEarned: Int,
)

/**
 * 魔力の購読（設計書03 §5）。行は回収・還元の書き込みで遅延生成されるため、
 * まだ行が無い間は 0 / 0 を流す（issue #50 の計画で決定）。
 */
class ManaRepository(
    private val database: SeireiDatabase,
) {
    val manaFlow: Flow<ManaBalance> =
        database.manaDao().observe().map { row -> row?.toDomain() ?: ManaBalance(0, 0) }
            .distinctUntilChanged()
}
