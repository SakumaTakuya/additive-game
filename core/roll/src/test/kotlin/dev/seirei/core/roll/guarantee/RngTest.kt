package dev.seirei.core.roll.guarantee

import dev.seirei.core.roll.Rng
import dev.seirei.core.roll.fnv1a64
import dev.seirei.core.roll.splitMix64
import dev.seirei.core.roll.subSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rng.kt の凍結を機械的に守るテスト（設計書02 §3・§8）。
 * 期待値は tools/rng_vectors.py（Kotlin と独立した参照実装）で生成した。
 * ここが落ちたら Rng.kt を直す。期待値を書き換えてはいけない。
 */
class RngTest {
    @Test fun `SplitMix64 の既知ベクタ（シード0）`() {
        val r = Rng(0)
        assertEquals(-2152535657050944081L, r.nextLong())   // 0xE220A8397B1DCDAF
        assertEquals(7960286522194355700L, r.nextLong())
        assertEquals(487617019471545679L, r.nextLong())
    }

    @Test fun `シード42の列`() {
        val r = Rng(42)
        assertEquals(-4767286540954276203L, r.nextLong())
        assertEquals(2949826092126892291L, r.nextLong())
        assertEquals(5139283748462763858L, r.nextLong())
    }

    @Test fun `splitMix64 は Rng の初出力と一致する`() {
        assertEquals(Rng(42).nextLong(), splitMix64(42))
    }

    @Test fun `nextDouble は上位53ビットで作る`() {
        assertEquals(0.7415648787718233, Rng(42).nextDouble(), 0.0)
    }

    @Test fun `nextDouble と nextInt の範囲`() {
        val r = Rng(7)
        repeat(10_000) {
            val d = r.nextDouble()
            assertTrue(d >= 0.0 && d < 1.0)
            val i = r.nextInt(12)
            assertTrue(i in 0 until 12)
        }
    }

    @Test fun `FNV-1a 64 の既知値（UTF-8）`() {
        assertEquals(8999218804125224858L, fnv1a64("encounter"))
        assertEquals(-5707848446072713286L, fnv1a64("quell"))
        assertEquals(6778608959930755579L, fnv1a64("精霊"))
    }

    @Test fun `subSeed の既知値`() {
        assertEquals(8738628067895197900L, subSeed(42, "encounter"))
        assertEquals(-2072697035987540750L, subSeed(42, "quell"))
        assertEquals(-6257538397141042427L, subSeed(42, "awaken.count"))
        assertEquals(5220664751412748491L, subSeed(42, "awaken.0.star"))
    }

    @Test fun `同一シードは同一列（決定論）`() {
        val a = Rng(123)
        val b = Rng(123)
        repeat(1_000) { assertEquals(a.nextLong(), b.nextLong()) }
    }

    @Test fun `ストリームラベルが異なれば独立（設計書02 §3）`() {
        assertNotEquals(subSeed(42, "encounter"), subSeed(42, "awaken.count"))
        assertNotEquals(subSeed(42, "awaken.0.star"), subSeed(42, "awaken.1.star"))
        assertNotEquals(subSeed(42, "awaken.0.star"), subSeed(42, "named.star"))
    }
}
