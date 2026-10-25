package dev.seirei.core.domain

import dev.seirei.core.model.ColorTier
import dev.seirei.core.model.Resident

/** Ports for returning residents to mana (design 03 §4.4). */
interface ReturnToManaStore {
    /** Reads the residents referenced by [ids]. The returned order is unspecified. */
    suspend fun loadByIds(ids: List<Long>): List<Resident>

    /** Resident IDs currently in the party; they can never be returned to mana. */
    suspend fun loadPartyIds(): List<Long>

    /**
     * Atomically deletes [residentIds] and adds [refund] mana. Must return false
     * without writing if any of the residents no longer exists, including when
     * concurrent calls race.
     */
    suspend fun returnToMana(residentIds: List<Long>, refund: Int): Boolean
}

sealed interface ReturnToManaOutcome {
    data class Returned(val refund: Int) : ReturnToManaOutcome
    data object NoOp : ReturnToManaOutcome
}

/**
 * Mana refunded for one resident (design 03 §4.4・§7):
 * 10 × 2^(star−1) × color multiplier (NORMAL ×1, RARE ×3, GLOW ×10).
 * TODO(DEC-03): provisional rates.
 */
fun manaRefundFor(resident: Resident): Int {
    val colorMultiplier = when (resident.colorTier) {
        ColorTier.NORMAL -> 1
        ColorTier.RARE -> 3
        ColorTier.GLOW -> 10
    }
    return 10 * (1 shl (resident.star - 1)) * colorMultiplier
}

/**
 * Returns the selected residents to mana (design 03 §4.4, manual selection only).
 * Locked residents and party members are always excluded. [preview] computes the
 * total without writing; the UI uses it before confirming.
 */
class ReturnToManaUseCase(
    private val store: ReturnToManaStore,
) {
    suspend fun preview(ids: List<Long>): Int =
        eligible(ids).sumOf(::manaRefundFor)

    suspend fun invoke(ids: List<Long>): ReturnToManaOutcome {
        val targets = eligible(ids)
        if (targets.isEmpty()) return ReturnToManaOutcome.NoOp

        val refund = targets.sumOf(::manaRefundFor)
        return if (store.returnToMana(targets.map(Resident::id), refund)) {
            ReturnToManaOutcome.Returned(refund)
        } else {
            ReturnToManaOutcome.NoOp
        }
    }

    private suspend fun eligible(ids: List<Long>): List<Resident> {
        val partyIds = store.loadPartyIds().toSet()
        return store.loadByIds(ids).filter { !it.locked && it.id !in partyIds }
    }
}
