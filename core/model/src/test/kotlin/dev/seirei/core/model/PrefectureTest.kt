package dev.seirei.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 都道府県と気象庁予報区コードの対応（設計書03 §2.1）。 */
class PrefectureTest {
    @Test
    fun `47都道府県の予報区コードは6桁で重複しない`() {
        assertEquals(47, Prefecture.entries.size)
        assertTrue(Prefecture.entries.all { it.forecastAreaCode.matches(Regex("\\d{6}")) })
        assertEquals(47, Prefecture.entries.map { it.forecastAreaCode }.toSet().size)
    }

    @Test
    fun `北海道鹿児島沖縄の代表予報区コード`() {
        assertEquals("016000", Prefecture.HOKKAIDO.forecastAreaCode)
        assertEquals("460100", Prefecture.KAGOSHIMA.forecastAreaCode)
        assertEquals("471000", Prefecture.OKINAWA.forecastAreaCode)
    }
}
