package dev.seirei.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

/**
 * 時間帯と天気グループ（設計書02 §2）。エンジンの修正値判定と Widget の空が同じ関数を使う。
 * 昼 05:00–16:59 ／ 夕 17:00–18:59 ／ 夜 19:00–04:59。雨と雪はまとめて WET、未取得は中立。
 */
class WeatherTest {

    // ---- 代表例 ----

    @Test fun `正午は昼、18時は夕方、22時は夜`() {
        assertEquals(TimeBand.DAY, timeBandOf(LocalTime.of(12, 0)))
        assertEquals(TimeBand.EVENING, timeBandOf(LocalTime.of(18, 0)))
        assertEquals(TimeBand.NIGHT, timeBandOf(LocalTime.of(22, 0)))
    }

    @Test fun `晴れ・曇りはそれぞれのグループ、雨と雪はまとめてWET`() {
        assertEquals(WeatherGroup.SUN, WeatherKind.SUNNY.group)
        assertEquals(WeatherGroup.CLOUD, WeatherKind.CLOUDY.group)
        assertEquals(WeatherGroup.WET, WeatherKind.RAIN.group)
        assertEquals(WeatherGroup.WET, WeatherKind.SNOW.group)
    }

    // ---- 境界 ----

    @Test fun `昼は5時ちょうどから、夕方は17時ちょうどから、夜は19時ちょうどから`() {
        assertEquals(TimeBand.NIGHT, timeBandOf(LocalTime.of(4, 59)))
        assertEquals(TimeBand.DAY, timeBandOf(LocalTime.of(5, 0)))
        assertEquals(TimeBand.DAY, timeBandOf(LocalTime.of(16, 59)))
        assertEquals(TimeBand.EVENING, timeBandOf(LocalTime.of(17, 0)))
        assertEquals(TimeBand.EVENING, timeBandOf(LocalTime.of(18, 59)))
        assertEquals(TimeBand.NIGHT, timeBandOf(LocalTime.of(19, 0)))
    }

    @Test fun `0時は夜`() {
        assertEquals(TimeBand.NIGHT, timeBandOf(LocalTime.of(0, 0)))
    }

    @Test fun `天気が未取得なら中立（天気の相性はどれも発動しない）`() {
        assertEquals(WeatherGroup.NONE, WeatherKind.UNKNOWN.group)
    }
}
