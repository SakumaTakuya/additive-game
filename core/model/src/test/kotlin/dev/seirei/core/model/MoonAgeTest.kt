package dev.seirei.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** 月齢近似式の暦との照合（設計書02 §5.3・§8）。 */
class MoonAgeTest {
    @Test
    fun `既知の満月日で月齢が参照値と一致する`() {
        val datesAndExpectedAges = listOf(
            LocalDate.of(2025, 9, 7) to 14.54,
            LocalDate.of(2026, 3, 3) to 14.36,
            LocalDate.of(2026, 8, 28) to 15.18,
        )

        datesAndExpectedAges.forEach { (date, expectedAge) ->
            assertEquals("$date", expectedAge, moonAge(date), 1.0)
        }
    }

    @Test
    fun `新月付近の日付で月齢が朔望月の端にある`() {
        val age = moonAge(LocalDate.of(2026, 10, 11))

        assertTrue(age in 0.0..1.5 || age in 28.5..29.6)
    }

    @Test
    fun `満月窓は月齢13以上17未満`() {
        assertTrue(isFullMoonWindow(LocalDate.of(2025, 9, 7)))
        assertTrue(!isFullMoonWindow(LocalDate.of(2026, 10, 11)))
    }
}
