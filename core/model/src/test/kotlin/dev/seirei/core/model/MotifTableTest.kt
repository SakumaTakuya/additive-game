package dev.seirei.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** モチーフ10種の表（設計書02 §5.3・設計書03 §3）。表とコードがずれたらコードを直す。 */
class MotifTableTest {
    @Test
    fun `モチーフIDは1から10まで重複なし`() {
        assertEquals((1..10).toList(), Motif.ALL.map { it.id }.sorted())
        assertEquals(10, Motif.ALL.map { it.id }.toSet().size)
    }

    @Test
    fun `モチーフごとの材質と大きさが設計書の表どおり`() {
        val expected = mapOf(
            1 to (Material.WATER to SizeClass.M),
            2 to (Material.WATER to SizeClass.S),
            3 to (Material.EARTHENWARE to SizeClass.M),
            4 to (Material.BONE to SizeClass.L),
            5 to (Material.STONE to SizeClass.S),
            6 to (Material.FIRE to SizeClass.M),
            7 to (Material.STONE to SizeClass.L),
            8 to (Material.STONE to SizeClass.S),
            9 to (Material.METAL to SizeClass.S),
            10 to (Material.PLANT to SizeClass.M),
        )

        assertEquals(expected, Motif.ALL.associate { it.id to (it.material to it.sizeClass) })
        assertTrue(Motif.ALL.all { it.displayName.isNotBlank() })
        assertEquals(mapOf(SizeClass.S to 48, SizeClass.M to 72, SizeClass.L to 120), SizeClass.entries.associateWith { it.dp })
    }
}
