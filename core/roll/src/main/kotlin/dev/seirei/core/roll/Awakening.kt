package dev.seirei.core.roll

import dev.seirei.core.model.ColorTier

/** One awakened resident's deterministic attributes (design 02 §4 ③④). */
data class ResidentRoll(
    val motifId: Int,
    val star: Int,
    val personalityId: Int,
    val color: ColorTier,
)

/** Rolls the guaranteed resident plus at most one battle reward. */
fun rollAwakened(
    seed: Long,
    quellCount: Int,
    earlyReturn: Boolean,
    constants: RollConstants,
): List<ResidentRoll> {
    require(quellCount >= 0) { "quellCount must be non-negative: $quellCount" }
    require(constants.quellNorm > 0) { "quellNorm must be positive" }
    require(constants.personalityCount > 0) { "personalityCount must be positive" }
    require(constants.awakenUpsideTable.isNotEmpty()) { "awakenUpsideTable must not be empty" }

    val upsideRate = constants.awakenUpsideTable[
        minOf(quellCount, constants.awakenUpsideTable.lastIndex),
    ]
    require(upsideRate in 0.0..1.0) { "upside rate must be in 0..1: $upsideRate" }
    val countRng = Rng(subSeed(seed, "awaken.count"))
    val count = 1 + if (!earlyReturn && countRng.nextDouble() < upsideRate) 1 else 0
    check(count >= 1)

    val t = minOf(1.0, quellCount.toDouble() / constants.quellNorm)
    val starWeights = doubleArrayOf(
        constants.starBase.first * (1.0 - t) + constants.starBoosted.first * t,
        constants.starBase.second * (1.0 - t) + constants.starBoosted.second * t,
        constants.starBase.third * (1.0 - t) + constants.starBoosted.third * t,
    )
    return List(count) { index -> rollResident(seed, index, starWeights, constants) }
}

private fun rollResident(
    seed: Long,
    index: Int,
    starBandWeights: DoubleArray,
    constants: RollConstants,
): ResidentRoll {
    val prefix = "awaken.$index"
    val motifId = Rng(subSeed(seed, "$prefix.motif")).nextInt(10) + 1
    val starRng = Rng(subSeed(seed, "$prefix.star"))
    val starBand = starRng.weightedIndex(starBandWeights)
    val star = if (starBand == 0) {
        starRng.weightedIndex(
            doubleArrayOf(
                constants.star1To3.first,
                constants.star1To3.second,
                constants.star1To3.third,
            ),
        ) + 1
    } else {
        starBand + 3
    }
    val personalityId = Rng(subSeed(seed, "$prefix.personality"))
        .nextInt(constants.personalityCount)
    val colorRng = Rng(subSeed(seed, "$prefix.color"))
    val color = when {
        colorRng.nextDouble() < constants.glowColorRate -> ColorTier.GLOW
        colorRng.nextDouble() < constants.rareColorRate -> ColorTier.RARE
        else -> ColorTier.NORMAL
    }
    return ResidentRoll(motifId, star, personalityId, color)
}
