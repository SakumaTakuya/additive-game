package dev.seirei.core.roll

import dev.seirei.core.model.ContextSnapshot
import dev.seirei.core.model.Party

/** The result for one encounter, in encounter order. */
data class EncounterOutcome(
    val encounter: Encounter,
    /** `true` is quelled, `false` is failed, and `null` means the encounter was skipped. */
    val success: Boolean?,
)

/** Battle results consumed by the later log and reward stages (design 02 §5.1). */
data class BattleOutcome(
    val encounters: List<EncounterOutcome>,
    val q: Int,
    val earlyReturn: Boolean,
)

/** Pluggable encounter resolution for the fixed pipeline (design 02 §5.1). */
interface BattleResolver {
    fun resolve(
        encounters: List<Encounter>,
        party: Party,
        snapshot: ContextSnapshot,
        rng: Rng,
    ): BattleOutcome
}

/** PoC resolver: each encounter succeeds independently with the party's shared win rate. */
class MinimalBattleResolver(
    private val constants: RollConstants,
) : BattleResolver {
    override fun resolve(
        encounters: List<Encounter>,
        party: Party,
        snapshot: ContextSnapshot,
        rng: Rng,
    ): BattleOutcome {
        val p = winRate(party, snapshot)
        val outcomes = encounters.map { encounter ->
            EncounterOutcome(encounter = encounter, success = rng.nextDouble() < p)
        }
        return BattleOutcome(
            encounters = outcomes,
            q = outcomes.count { it.success == true },
            earlyReturn = false, // TODO(DEC-04): 叩き台。PoC では早期帰還を起こさない。
        )
    }

    internal fun winRate(party: Party, snapshot: ContextSnapshot): Double {
        val quellArrows = quellArrowsFor(party, snapshot)
        val rawRate = party.members.indices.sumOf { slot ->
            val member = party.members[slot]
            constants.memberBase +
                constants.starPt * (member.star - 1) +
                constants.arrowPt * quellArrows[slot]
        }
        return minOf(constants.pMax, maxOf(0.0, rawRate))
    }

    /** Shared with the UI preview through the canonical [arrowsFor] rule. */
    internal fun quellArrowsFor(party: Party, snapshot: ContextSnapshot): List<Int> =
        party.members.indices.map { slot -> arrowsFor(slot, party, snapshot).quell }
}

/** Debug resolver that treats every encounter as unquelled without consuming randomness. */
object NoBattleResolver : BattleResolver {
    override fun resolve(
        encounters: List<Encounter>,
        party: Party,
        snapshot: ContextSnapshot,
        rng: Rng,
    ): BattleOutcome {
        val outcomes = encounters.map { encounter ->
            EncounterOutcome(encounter = encounter, success = null)
        }
        return BattleOutcome(encounters = outcomes, q = 0, earlyReturn = false)
    }
}
