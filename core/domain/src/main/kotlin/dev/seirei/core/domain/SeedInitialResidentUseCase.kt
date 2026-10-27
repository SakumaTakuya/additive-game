package dev.seirei.core.domain

import dev.seirei.core.model.Clock
import dev.seirei.core.roll.ResidentRoll
import dev.seirei.core.roll.RollConstants
import dev.seirei.core.roll.resolveNamed
import java.time.Instant

/** Ports for the initial grant (design 03 §4.6). */
interface SeedInitialResidentStore {
    suspend fun countResidents(): Int

    /**
     * Atomically inserts the resident for [roll] (source `INITIAL_GIFT_SOURCE`).
     * The §4.6 guard is rechecked inside the same transaction: it must return
     * false without writing if a resident already exists or a session row has
     * appeared since the preview, including when concurrent calls race.
     */
    suspend fun insertInitial(roll: ResidentRoll, acquiredAt: Instant): Boolean
}

sealed interface SeedInitialOutcome {
    data class Seeded(val roll: ResidentRoll) : SeedInitialOutcome
    data object NoOp : SeedInitialOutcome
}

/**
 * Initial grant on first launch (design 03 §4.6, DEC-11). Only when there are no
 * residents and no session row at all (not even a collected one): grants one
 * resident of motif #1 through [resolveNamed], with no star guarantee.
 * Idempotent — a second call observes the granted resident and does nothing.
 */
class SeedInitialResidentUseCase(
    private val clock: Clock,
    private val constants: RollConstants,
    private val sessionStore: SessionStore,
    private val store: SeedInitialResidentStore,
    private val seedSource: SeedSource,
) {
    suspend fun invoke(): SeedInitialOutcome {
        if (store.countResidents() > 0) return SeedInitialOutcome.NoOp
        if (sessionStore.loadCurrent() != null) return SeedInitialOutcome.NoOp

        val roll = resolveNamed(seedSource.nextSeed(), motifId = 1, constants)
        return if (store.insertInitial(roll, clock.now())) {
            SeedInitialOutcome.Seeded(roll)
        } else {
            SeedInitialOutcome.NoOp
        }
    }
}
