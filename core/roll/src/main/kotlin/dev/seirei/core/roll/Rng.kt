package dev.seirei.core.roll

/*
 * 凍結ファイル（設計書02 §3）。このファイルのアルゴリズム・定数・関数の意味を変えると、
 * すべてのゴールデンテストと過去セッションの再現性が壊れる。変更には design-change ラベルが必要。
 *
 * kotlin.random.Random と java.util.Random は使わない（言語・JDK更新で列が変わりうるため）。
 */

private const val GOLDEN_GAMMA: Long = -0x61c8864680b583ebL   // 0x9E3779B97F4A7C15
private const val MIX_1: Long = -0x40a7b892e31b1a47L          // 0xBF58476D1CE4E5B9
private const val MIX_2: Long = -0x6b2fb644ecceee15L          // 0x94D049BB133111EB
private const val FNV_OFFSET: Long = -0x340d631b7bdddcdbL     // 0xCBF29CE484222325
private const val FNV_PRIME: Long = 0x100000001b3L

/** SplitMix64 の出力関数。状態 x を1歩進めた値を混合して返す（= Rng(x).nextLong()）。 */
fun splitMix64(x: Long): Long {
    var z = x + GOLDEN_GAMMA
    z = (z xor (z ushr 30)) * MIX_1
    z = (z xor (z ushr 27)) * MIX_2
    return z xor (z ushr 31)
}

/** FNV-1a 64bit（UTF-8 バイト列）。ストリームラベルのハッシュに使う。 */
fun fnv1a64(label: String): Long {
    var h = FNV_OFFSET
    for (b in label.encodeToByteArray()) {
        h = h xor (b.toLong() and 0xff)
        h *= FNV_PRIME
    }
    return h
}

/**
 * サブシステムごとの独立ストリーム（設計書02 §3）。
 * ラベル文字列（"encounter", "quell", "awaken.count", "awaken.$i.star", "named.star" など）は凍結。
 */
fun subSeed(seed: Long, label: String): Long = splitMix64(seed xor fnv1a64(label))

/** SplitMix64 生成器。状態を持つが、同じシードからは常に同じ列を返す。 */
class Rng(seed: Long) {
    private var state: Long = seed

    fun nextLong(): Long {
        state += GOLDEN_GAMMA
        var z = state
        z = (z xor (z ushr 30)) * MIX_1
        z = (z xor (z ushr 27)) * MIX_2
        return z xor (z ushr 31)
    }

    /** [0, 1) の一様乱数。上位53ビットを使う。 */
    fun nextDouble(): Double = (nextLong() ushr 11) * (1.0 / (1L shl 53))

    /** [0, bound) の整数。bound > 0。floor(nextDouble() * bound) で定義する（わずかな偏りは許容）。 */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive: $bound" }
        return (nextDouble() * bound).toInt()
    }
}
