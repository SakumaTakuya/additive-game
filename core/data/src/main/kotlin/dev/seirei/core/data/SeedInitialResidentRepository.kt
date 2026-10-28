package dev.seirei.core.data

import dev.seirei.core.database.SeireiDatabase
import dev.seirei.core.domain.SeedInitialResidentStore
import dev.seirei.core.model.INITIAL_GIFT_SOURCE
import dev.seirei.core.roll.ResidentRoll
import java.time.Instant

/**
 * [SeedInitialResidentStore] の Room 実装（設計書03 §4.6）。§4.6 のガードの再チェックは
 * SeedInitialDao のトランザクション内で行われる。
 */
class SeedInitialResidentRepository(
    private val database: SeireiDatabase,
) : SeedInitialResidentStore {
    override suspend fun countResidents(): Int =
        database.seedInitialDao().countResidents()

    override suspend fun insertInitial(roll: ResidentRoll, acquiredAt: Instant): Boolean =
        database.seedInitialDao().insertInitial(
            roll.toEntity(sourceSessionId = INITIAL_GIFT_SOURCE, acquiredAt = acquiredAt),
        )
}
