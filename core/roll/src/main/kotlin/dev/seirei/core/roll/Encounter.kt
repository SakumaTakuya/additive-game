package dev.seirei.core.roll

/** One generated encounter, ordered by its offset within the expedition. */
data class Encounter(
    val index: Int,
    val motifId: Int,
    val atOffsetMin: Int,
)

/** Generates a deterministic encounter sequence for one expedition (design 02 §4 ①). */
fun generateEncounters(seed: Long, constants: RollConstants): List<Encounter> {
    require(constants.expeditionMinutes > 0) {
        "expeditionMinutes must be positive: ${constants.expeditionMinutes}"
    }

    val rng = Rng(subSeed(seed, "encounter"))
    val encounterCount = rng.poisson(
        constants.encounterRatePerHour * constants.expeditionMinutes / 60.0,
    )

    // Draw order is frozen for reproducibility: count → all motifs → all offsets.
    val motifIds = List(encounterCount) { rng.nextInt(10) + 1 }
    val offsets = List(encounterCount) { rng.nextInt(constants.expeditionMinutes) }

    return motifIds.zip(offsets)
        .map { (motifId, offset) -> motifId to offset }
        .sortedBy { (_, offset) -> offset }
        .mapIndexed { index, (motifId, offset) ->
            Encounter(index = index, motifId = motifId, atOffsetMin = offset)
        }
}
