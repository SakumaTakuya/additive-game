package dev.seirei.core.model

import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test

/** 材質7種の表（設計書02 §5.3）。表とコードがずれたらコードを直す。 */
class MaterialTableTest {
    @Test
    fun `材質ごとの得意な天気・苦手な天気・祭壇の曜日が設計書の表どおり（苦手な天気があるのは火だけ）`() {
        val expected = listOf(
            Material.WATER to Triple(WeatherGroup.WET, null, DayOfWeek.WEDNESDAY),
            Material.EARTHENWARE to Triple(WeatherGroup.WET, null, DayOfWeek.SATURDAY),
            Material.STONE to Triple(WeatherGroup.CLOUD, null, DayOfWeek.SUNDAY),
            Material.BONE to Triple(WeatherGroup.CLOUD, null, DayOfWeek.MONDAY),
            Material.METAL to Triple(WeatherGroup.SUN, null, DayOfWeek.FRIDAY),
            Material.FIRE to Triple(WeatherGroup.SUN, WeatherGroup.WET, DayOfWeek.TUESDAY),
            Material.PLANT to Triple(WeatherGroup.SUN, null, DayOfWeek.THURSDAY),
        )

        assertEquals(expected, Material.entries.map { it to Triple(it.goodWeather, it.badWeather, it.shrineDay) })
        assertEquals(
            listOf(Material.FIRE),
            Material.entries.filter { it.badWeather != null },
        )
    }

    @Test
    fun `天気ごとに得意なモチーフの数は晴れ3・曇り4・雨3`() {
        val counts = WeatherGroup.entries
            .filter { it != WeatherGroup.NONE }
            .associateWith { group -> Motif.ALL.count { it.material.goodWeather == group } }

        assertEquals(mapOf(WeatherGroup.SUN to 3, WeatherGroup.CLOUD to 4, WeatherGroup.WET to 3), counts)
    }
}
