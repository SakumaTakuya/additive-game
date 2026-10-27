package dev.seirei.core.data

import dev.seirei.core.database.DuplicateCollectionException
import dev.seirei.core.database.ResidentEntity
import dev.seirei.core.database.SeireiDatabase
import dev.seirei.core.domain.CollectStore
import dev.seirei.core.domain.CollectWriteResult
import dev.seirei.core.model.Clock
import dev.seirei.core.model.ExplorationSession
import dev.seirei.core.roll.ExpeditionResult
import dev.seirei.core.roll.ResidentRoll

/**
 * [CollectStore] backed by the Room collect transaction (design 03 §4.1).
 * Returns [CollectWriteResult.ALREADY_COLLECTED] without writing when the given
 * session is no longer the current row, is already collected, or already has a
 * receipt — including a duplicate that only surfaces inside the transaction.
 */
class CollectRepository(
    private val database: SeireiDatabase,
    private val clock: Clock,
) : CollectStore {
    override suspend fun collect(
        session: ExplorationSession,
        result: ExpeditionResult,
    ): CollectWriteResult {
        val current = database.sessionDao().get()
        if (current == null || current.id != session.id || current.collected) {
            return CollectWriteResult.ALREADY_COLLECTED
        }
        if (database.collectionReceiptDao().exists(session.id)) {
            return CollectWriteResult.ALREADY_COLLECTED
        }
        return try {
            database.collectDao().collect(
                sessionId = session.id,
                awakened = result.awakened.map { roll -> roll.toEntity(sourceSessionId = session.id) },
                manaGained = result.manaGained,
            )
            CollectWriteResult.COLLECTED
        } catch (e: DuplicateCollectionException) {
            CollectWriteResult.ALREADY_COLLECTED
        }
    }

    // acquiredAt is the moment of the write, read from the injected Clock
    // (ADR D-007); the domain port stays free of it because the roll itself
    // carries no timestamp.
    private fun ResidentRoll.toEntity(sourceSessionId: Long): ResidentEntity = ResidentEntity(
        motifId = motifId,
        star = star,
        personalityId = personalityId,
        colorTier = color,
        acquiredAt = clock.now(),
        sourceSessionId = sourceSessionId,
    )
}
