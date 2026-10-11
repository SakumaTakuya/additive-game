package dev.seirei.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

/** 時間帯の境界と天気グループ（設計書02 §2）。 */
class WeatherTest {
    @Test fun `時間帯の境界`() {
        assertEquals(TimeBand.NIGHT, timeBandOf(LocalTime.of(4, 59)))
        assertEquals(TimeBand.DAY, timeBandOf(LocalTime.of(5, 0)))
        assertEquals(TimeBand.DAY, timeBandOf(LocalTime.of(16, 59)))
        assertEquals(TimeBand.EVENING, timeBandOf(LocalTime.of(17, 0)))
        assertEquals(TimeBand.EVENING, timeBandOf(LocalTime.of(18, 59)))
        assertEquals(TimeBand.NIGHT, timeBandOf(LocalTime.of(19, 0)))
        assertEquals(TimeBand.NIGHT, timeBandOf(LocalTime.of(0, 0)))
    }

    @Test fun `雨と雪は WET、UNKNOWN は中立`() {
        assertEquals(WeatherGroup.SUN, WeatherKind.SUNNY.group)
        assertEquals(WeatherGroup.CLOUD, WeatherKind.CLOUDY.group)
        assertEquals(WeatherGroup.WET, WeatherKind.RAIN.group)
        assertEquals(WeatherGroup.WET, WeatherKind.SNOW.group)
        assertEquals(WeatherGroup.NONE, WeatherKind.UNKNOWN.group)
    }
}
