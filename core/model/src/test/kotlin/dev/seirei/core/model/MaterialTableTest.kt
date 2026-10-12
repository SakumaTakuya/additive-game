package dev.seirei.core.model

import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test

class MaterialTableTest {
    @Test
    fun materialWeatherAndShrineTableMatchesDesign() {
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
    }

    @Test
    fun favorableMotifCountsMatchDesign() {
        val counts = WeatherGroup.entries
            .filter { it != WeatherGroup.NONE }
            .associateWith { group -> Motif.ALL.count { it.material.goodWeather == group } }

        assertEquals(mapOf(WeatherGroup.SUN to 3, WeatherGroup.CLOUD to 4, WeatherGroup.WET to 3), counts)
    }
}
