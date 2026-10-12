package dev.seirei.core.roll

/** Three ordered probability weights, used for ★1–3 / ★4 / ★5 and ★1 / ★2 / ★3. */
data class Weights3(val first: Double, val second: Double, val third: Double) {
    fun sum(): Double = first + second + third
}

/** Immutable tuning values for one build flavor (design 02 §6). */
data class RollConstants(
    val starBase: Weights3,
    val starBoosted: Weights3,
    val star1To3: Weights3,
    val quellNorm: Int,
    val personalityCount: Int,
    val rareColorRate: Double,
    val glowColorRate: Double,
    val awakenUpsideTable: List<Double>,
    val encounterRatePerHour: Double,
    val manaPerQuell: Int,
    val manaBase: Int,
    val memberBase: Double,
    val starPt: Double,
    val arrowPt: Double,
    val pMax: Double,
    val manaArrowRate: Double,
    val expeditionMinutes: Int,
) {
    companion object {
        val POC = RollConstants(
            starBase = Weights3(0.85, 0.12, 0.03),
            starBoosted = Weights3(0.70, 0.20, 0.10),
            // TODO(DEC-01): 暫定値。★1:★2:★3 = 50:30:20
            star1To3 = Weights3(0.50, 0.30, 0.20),
            quellNorm = 5,
            personalityCount = 12,
            rareColorRate = 0.08,
            glowColorRate = 0.01,
            // TODO(DEC-02): 暫定値。鎮め数ごとの追加覚醒確率
            awakenUpsideTable = listOf(0.0, 0.0, 0.0, 0.25, 0.40, 0.50),
            encounterRatePerHour = 3.0,
            // TODO(DEC-03): 暫定値
            manaPerQuell = 10,
            manaBase = 20,
            memberBase = 0.12,
            starPt = 0.02,
            arrowPt = 0.05,
            pMax = 0.90,
            manaArrowRate = 0.10,
            expeditionMinutes = 120,
        )

        val DEMO = RollConstants(
            // TODO(DEC-05): 暫定値
            starBase = Weights3(0.60, 0.20, 0.20),
            starBoosted = Weights3(0.40, 0.30, 0.30),
            // TODO(DEC-01): 暫定値。★1:★2:★3 = 50:30:20
            star1To3 = Weights3(0.50, 0.30, 0.20),
            quellNorm = 5,
            personalityCount = 12,
            // TODO(DEC-05): 暫定値
            rareColorRate = 0.15,
            glowColorRate = 0.10,
            // TODO(DEC-02): 暫定値
            awakenUpsideTable = listOf(0.0, 0.5, 0.5, 0.5, 0.5, 0.5),
            encounterRatePerHour = 120.0,
            // TODO(DEC-03): 暫定値
            manaPerQuell = 10,
            manaBase = 20,
            memberBase = 0.12,
            starPt = 0.02,
            arrowPt = 0.05,
            pMax = 0.90,
            manaArrowRate = 0.10,
            expeditionMinutes = 3,
        )
    }
}
