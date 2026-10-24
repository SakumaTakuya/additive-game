package dev.seirei.core.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/** 曜日祭壇（設計書02 §5.3）。曜日ごとに祀られる材質が1つ決まる。 */
class ShrineTest {
    @Test fun `月曜から日曜まで、骨・火・水・植物・金属・土器・石の祭壇`() {
        val monday = LocalDate.of(2026, 10, 12)

        assertEquals(
            listOf(
                Material.BONE,
                Material.FIRE,
                Material.WATER,
                Material.PLANT,
                Material.METAL,
                Material.EARTHENWARE,
                Material.STONE,
            ),
            (0L..6L).map { shrineMaterialOf(monday.plusDays(it)) },
        )
    }
}
