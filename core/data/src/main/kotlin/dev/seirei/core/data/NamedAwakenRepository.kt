package dev.seirei.core.data

import dev.seirei.core.database.SeireiDatabase
import dev.seirei.core.domain.NamedAwakenStore
import dev.seirei.core.model.NAMED_SOURCE
import dev.seirei.core.roll.ResidentRoll
import java.time.Instant

/**
 * [NamedAwakenStore] の Room 実装（設計書03 §4.3）。残高と発見状態の再チェックは
 * NamedAwakenDao のトランザクション内で行われる。
 */
class NamedAwakenRepository(
    private val database: SeireiDatabase,
) : NamedAwakenStore {
    override suspend fun loadBalance(): Int =
        database.manaDao().get()?.balance ?: 0

    override suspend fun loadDiscoveredMotifIds(): Set<Int> =
        database.namedAwakenDao().discoveredMotifIds().toSet()

    override suspend fun awaken(roll: ResidentRoll, acquiredAt: Instant, cost: Int): Boolean =
        database.namedAwakenDao().awaken(
            resident = roll.toEntity(sourceSessionId = NAMED_SOURCE, acquiredAt = acquiredAt),
            cost = cost,
        )
}
