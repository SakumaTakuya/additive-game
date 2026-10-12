package dev.seirei.core.roll

import kotlin.math.ln

/** Knuth's Poisson sampler, consuming only this RNG's deterministic [Rng.nextDouble] stream. */
fun Rng.poisson(lambda: Double): Int {
    require(lambda.isFinite() && lambda >= 0.0) { "lambda must be finite and non-negative: $lambda" }
    if (lambda == 0.0) return 0

    // Accumulating logs is equivalent to Knuth's product test without underflowing
    // either the threshold exp(-lambda) or the running product for large lambda.
    var logProduct = 0.0
    var count = 0
    do {
        count++
        logProduct += ln(nextDouble())
    } while (logProduct > -lambda)
    return count - 1
}

/** Selects an index in proportion to its non-negative weight. */
fun Rng.weightedIndex(weights: DoubleArray): Int {
    require(weights.isNotEmpty()) { "weights must not be empty" }
    require(weights.all { it.isFinite() && it >= 0.0 }) {
        "weights must be finite and non-negative"
    }
    val total = weights.sum()
    require(total.isFinite() && total > 0.0) { "weights must have a finite positive sum" }

    val target = nextDouble() * total
    var cumulative = 0.0
    weights.forEachIndexed { index, weight ->
        cumulative += weight
        if (target < cumulative) return index
    }

    // Floating-point rounding can leave target equal to the accumulated total.
    return weights.indexOfLast { it > 0.0 }
}
