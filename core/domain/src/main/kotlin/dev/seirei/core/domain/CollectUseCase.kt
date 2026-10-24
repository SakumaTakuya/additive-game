package dev.seirei.core.domain

import dev.seirei.core.model.Clock
import dev.seirei.core.model.ExplorationSession
import dev.seirei.core.model.Party
import dev.seirei.core.model.PartyMember
import dev.seirei.core.model.Resident
import dev.seirei.core.model.SessionPhase
import dev.seirei.core.model.derivePhase
import dev.seirei.core.roll.ExpeditionResult
import dev.seirei.core.roll.RollConstants
import dev.seirei.core.roll.RollEngine

/** Reads residents referenced by the persisted party IDs. The returned order is unspecified. */
fun interface CollectResidentStore {
    suspend fun loadByIds(ids: List<Long>): List<Resident>
}

/**
 * Atomically records the receipt, rewards, and collected flag (design 03 §4.1).
 * Must return [CollectWriteResult.ALREADY_COLLECTED] without writing if this session is no
 * longer current or already has a receipt, including when concurrent calls race.
 */
fun interface CollectStore {
    suspend fun collect(session: ExplorationSession, result: ExpeditionResult): CollectWriteResult
}

enum class CollectWriteResult {
    COLLECTED,
    ALREADY_COLLECTED,
}

sealed interface CollectOutcome {
    data class Collected(val result: ExpeditionResult) : CollectOutcome
    data object NoOp : CollectOutcome
}

/**
 * Collects only when a fresh read of persisted state derives to RETURNED (design 01 §5).
 * The clock is read after each suspending store read, so a rollback during I/O is reevaluated
 * before any roll or write is performed.
 */
class CollectUseCase(
    private val clock: Clock,
    private val constants: RollConstants,
    private val sessionStore: SessionStore,
    private val residentStore: CollectResidentStore,
    private val collectStore: CollectStore,
) {
    suspend fun invoke(): CollectOutcome {
        val session = sessionStore.loadCurrent()
        if (derivePhase(session, clock.now()) != SessionPhase.RETURNED) return CollectOutcome.NoOp
        checkNotNull(session)

        val residents = residentStore.loadByIds(session.partyIds).associateBy(Resident::id)
        if (derivePhase(session, clock.now()) != SessionPhase.RETURNED) return CollectOutcome.NoOp
        val party = Party(session.partyIds.map { id ->
            val resident = checkNotNull(residents[id]) {
                "Persisted party references missing resident $id"
            }
            PartyMember(
                motifId = resident.motifId,
                star = resident.star,
                personalityId = resident.personalityId,
            )
        })
        val result = RollEngine.resolve(session.seed, session.snapshot, party, constants)
        if (derivePhase(session, clock.now()) != SessionPhase.RETURNED) return CollectOutcome.NoOp

        return when (collectStore.collect(session, result)) {
            CollectWriteResult.COLLECTED -> CollectOutcome.Collected(result)
            CollectWriteResult.ALREADY_COLLECTED -> CollectOutcome.NoOp
        }
    }
}
