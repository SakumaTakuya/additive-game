package dev.seirei.core.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ShrineTest {
    @Test
    fun shrineMaterialForWeekOfOctoberTwelfth2026() {
        val expected = listOf(
            Material.BONE,
            Material.FIRE,
            Material.WATER,
            Material.PLANT,
            Material.METAL,
            Material.EARTHENWARE,
            Material.STONE,
        )

        assertEquals(expected, (0L..6L).map { shrineMaterialOf(LocalDate.of(2026, 10, 12).plusDays(it)) })
    }
}
