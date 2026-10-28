package dev.seirei.core.data

import dev.seirei.core.database.ReturnTargetRow
import dev.seirei.core.database.SeireiDatabase
import dev.seirei.core.domain.ReturnTarget
import dev.seirei.core.domain.ReturnToManaStore
import dev.seirei.core.model.Resident

/**
 * [ReturnToManaStore] の Room 実装（設計書03 §4.4）。locked・編成中の再チェックは
 * ReturnToManaDao のトランザクション内で行われ、実際に還元された合計だけが返る。
 */
class ReturnToManaRepository(
    private val database: SeireiDatabase,
) : ReturnToManaStore {
    override suspend fun loadByIds(ids: List<Long>): List<Resident> =
        ids.mapNotNull { id -> database.residentDao().getById(id)?.toDomain() }

    override suspend fun loadPartyIds(): List<Long> =
        database.sessionDao().get()?.partyIds.orEmpty()

    override suspend fun returnToMana(targets: List<ReturnTarget>): Int =
        database.returnToManaDao().returnToMana(
            targets.map { target -> ReturnTargetRow(target.resident.id, target.refund) },
        )
}
