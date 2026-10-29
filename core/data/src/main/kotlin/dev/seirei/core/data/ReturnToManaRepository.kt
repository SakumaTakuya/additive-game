package dev.seirei.core.data

import androidx.room.withTransaction
import dev.seirei.core.database.ReturnTargetRow
import dev.seirei.core.database.SeireiDatabase
import dev.seirei.core.domain.ReturnTarget
import dev.seirei.core.domain.ReturnToManaStore
import dev.seirei.core.model.Resident

/**
 * [ReturnToManaStore] の Room 実装（設計書03 §4.4）。locked・編成中の再チェックは
 * ReturnToManaDao のトランザクション内で行われ、実際に還元された合計だけが返る。
 * 個体削除時のプリセット整合（設計書03 §3）はこの層で取る：還元と同一の
 * トランザクションで、プリセット（セッション行 partyIds）から削除済みの住人を除く。
 * 書き込み時点でスキップされた住人は残っているのでプリセットにも残る。
 */
class ReturnToManaRepository(
    private val database: SeireiDatabase,
) : ReturnToManaStore {
    override suspend fun loadByIds(ids: List<Long>): List<Resident> =
        ids.mapNotNull { id -> database.residentDao().getById(id)?.toDomain() }

    override suspend fun loadPartyIds(): List<Long> =
        database.sessionDao().get()?.partyIds.orEmpty()

    override suspend fun returnToMana(targets: List<ReturnTarget>): Int =
        database.withTransaction {
            val refunded = database.returnToManaDao().returnToMana(
                targets.map { target -> ReturnTargetRow(target.resident.id, target.refund) },
            )
            removeDeletedResidentsFromPreset()
            refunded
        }

    /** プリセットに残る ID のうち、住人そのものが消えているものを除去する。 */
    private suspend fun removeDeletedResidentsFromPreset() {
        val session = database.sessionDao().get() ?: return
        if (session.partyIds.isEmpty()) return
        val kept = session.partyIds.filter { id -> database.residentDao().getById(id) != null }
        if (kept != session.partyIds) {
            database.sessionDao().upsert(session.copy(partyIds = kept))
        }
    }
}
