package dev.seirei.core.roll

import dev.seirei.core.model.ContextSnapshot
import dev.seirei.core.model.Party
import kotlin.math.floor

/** Structured events for one expedition (design 02 §4). */
sealed interface LogEvent {
    data class Encountered(val index: Int, val motifId: Int, val atOffsetMin: Int) : LogEvent
    data class Quelled(val index: Int) : LogEvent
    data class Failed(val index: Int) : LogEvent
    data class Found(val index: Int, val mana: Int) : LogEvent
    data class Awakened(val rollIndex: Int) : LogEvent
}

/** Complete deterministic output for one expedition. */
data class ExpeditionResult(
    val log: List<LogEvent>,
    val awakened: List<ResidentRoll>,
    val manaGained: Int,
    val earlyReturn: Boolean,
)

/** Runs the fixed encounter → battle → awakening → log and reward pipeline. */
object RollEngine {
    fun resolve(
        seed: Long,
        snapshot: ContextSnapshot,
        party: Party,
        constants: RollConstants,
    ): ExpeditionResult {
        val encounters = generateEncounters(seed, constants)
        val battleOutcome = MinimalBattleResolver(constants).resolve(
            encounters = encounters,
            party = party,
            snapshot = snapshot,
            rng = Rng(subSeed(seed, "quell")),
        )
        require(battleOutcome.encounters.map(EncounterOutcome::encounter) == encounters) {
            "BattleResolver must return one ordered outcome for every encounter"
        }
        require(battleOutcome.q == battleOutcome.encounters.count { it.success == true }) {
            "BattleResolver q must match its successful encounter count"
        }

        val awakened = rollAwakened(seed, battleOutcome.q, battleOutcome.earlyReturn, constants)
        val manaArrowTotal = party.members.indices.sumOf { slot -> arrowsFor(slot, party, snapshot).mana }
        val manaMultiplier = 1.0 + constants.manaArrowRate * manaArrowTotal
        val manaGained = floor(
            (constants.manaBase + constants.manaPerQuell * battleOutcome.q) * manaMultiplier,
        ).toInt()

        val log = buildList {
            addAll(battleLogEvents(battleOutcome.encounters, constants.manaPerQuell))
            awakened.indices.forEach { add(LogEvent.Awakened(it)) }
        }

        return ExpeditionResult(
            log = log,
            awakened = awakened,
            manaGained = manaGained,
            earlyReturn = battleOutcome.earlyReturn,
        )
    }
}

/**
 * A skipped encounter has no outcome event. The frozen LogEvent schema has no Skipped type;
 * retaining Encountered without Quelled/Failed keeps it distinguishable from a failed battle.
 */
internal fun battleLogEvents(outcomes: List<EncounterOutcome>, manaPerQuell: Int): List<LogEvent> =
    buildList {
        outcomes.forEach { outcome ->
            val encounter = outcome.encounter
            add(LogEvent.Encountered(encounter.index, encounter.motifId, encounter.atOffsetMin))
            when (outcome.success) {
                true -> {
                    add(LogEvent.Quelled(encounter.index))
                    add(LogEvent.Found(encounter.index, manaPerQuell))
                }
                false -> add(LogEvent.Failed(encounter.index))
                null -> Unit
            }
        }
    }
