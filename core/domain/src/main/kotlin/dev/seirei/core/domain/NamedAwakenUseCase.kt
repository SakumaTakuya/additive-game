package dev.seirei.core.domain

import dev.seirei.core.model.Clock
import dev.seirei.core.roll.ResidentRoll
import dev.seirei.core.roll.RollConstants
import dev.seirei.core.roll.resolveNamed
import java.time.Instant

/** TODO(DEC-03): provisional ceiling cost of a named awakening (design 03 §7). */
const val CEILING_COST: Int = 400

/** Ports for the named-awakening transaction (design 03 §4.3). */
interface NamedAwakenStore {
    suspend fun loadBalance(): Int

    /** Motifs the player has already discovered and may therefore name. */
    suspend fun loadDiscoveredMotifIds(): Set<Int>

    /**
     * Atomically inserts the resident for [roll] (source `NAMED_SOURCE`) and subtracts
     * [cost] mana. The guards are rechecked inside the same transaction: it must
     * return false without writing if the balance no longer covers [cost] or the
     * motif is no longer discovered at write time, including when concurrent
     * calls race.
     */
    suspend fun awaken(roll: ResidentRoll, acquiredAt: Instant, cost: Int): Boolean
}

sealed interface NamedAwakenOutcome {
    data class Awakened(val roll: ResidentRoll) : NamedAwakenOutcome
    data object NoOp : NamedAwakenOutcome
}

/**
 * Named awakening (design 03 §4.3). Only when the balance covers [CEILING_COST] and
 * the motif is already discovered: resolves the roll with [resolveNamed] (motif fixed,
 * star/personality/color rolled normally) and spends the mana. Otherwise does nothing,
 * with no exception and no notification (domain AGENTS.md).
 */
class NamedAwakenUseCase(
    private val clock: Clock,
    private val constants: RollConstants,
    private val store: NamedAwakenStore,
    private val seedSource: SeedSource,
) {
    suspend fun invoke(motifId: Int): NamedAwakenOutcome {
        if (motifId !in store.loadDiscoveredMotifIds()) return NamedAwakenOutcome.NoOp
        if (store.loadBalance() < CEILING_COST) return NamedAwakenOutcome.NoOp

        val roll = resolveNamed(seedSource.nextSeed(), motifId, constants)
        return if (store.awaken(roll, clock.now(), CEILING_COST)) {
            NamedAwakenOutcome.Awakened(roll)
        } else {
            NamedAwakenOutcome.NoOp
        }
    }
}
