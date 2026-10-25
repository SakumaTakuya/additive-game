package dev.seirei.core.roll

import dev.seirei.core.model.Material
import dev.seirei.core.model.Personality
import dev.seirei.core.model.TimeBand
import dev.seirei.core.model.WeatherKind
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * 修正値（矢印）の判定 `arrowsFor`（設計書02 §5.3）。編成画面のプレビューと鎮め解決が同じ関数を使う。
 *
 * 矢印の出どころは4つ：材質×天気、材質×曜日祭壇、性格の条件、先導役の隊長。
 * 判定はすべて点火時のスナップショットで行う。
 * ここでは規則ごとの代表例を示す。全組み合わせの照合は guarantee/ArrowsTableTest。
 */
class ArrowsSpecTest {

    // ---- 代表例 ----

    @Test fun `雨の日の雨好きな水の住人は、材質の相性+1と性格+2で鎮めの矢印3本`() {
        val party = partyOf(member(Material.WATER, 3, Personality.RAIN_LOVING))

        assertEquals(
            Arrows(quell = 3, mana = 0, sources = listOf(ArrowSource("雨", 1, 0), ArrowSource("雨好き・雨", 2, 0))),
            arrowsFor(0, party, snapshot(weather = WeatherKind.RAIN)),
        )
    }

    // ---- 材質 × 天気 ----

    @Test fun `雪は雨と同じ扱い`() {
        val party = partyOf(member(Material.WATER, 3, Personality.NOCTURNAL))

        assertEquals(listOf(ArrowSource("雨", 1, 0)), arrowsFor(0, party, snapshot(weather = WeatherKind.SNOW)).sources)
    }

    @Test fun `火の住人だけは雨の日に矢印−1`() {
        val party = partyOf(member(Material.FIRE, 3, Personality.NOCTURNAL))

        assertEquals(listOf(ArrowSource("雨", -1, 0)), arrowsFor(0, party, snapshot(weather = WeatherKind.RAIN)).sources)
    }

    @Test fun `天気が未取得なら天気の相性はどれも発動しない`() {
        val party = partyOf(member(Material.METAL, 3, Personality.SUN_LOVING))

        assertEquals(Arrows(0, 0, emptyList()), arrowsFor(0, party, snapshot(weather = WeatherKind.UNKNOWN)))
    }

    // ---- 材質 × 曜日祭壇 ----

    @Test fun `木曜は植物の祭壇の日で、植物の住人に+1`() {
        val party = partyOf(member(Material.PLANT, 3, Personality.NOCTURNAL))

        assertEquals(listOf(ArrowSource("木曜の祭壇", 1, 0)), arrowsFor(0, party, snapshot(day = DayOfWeek.THURSDAY)).sources)
    }

    @Test fun `信心深い住人は自分の祭壇の日にさらに+3で、合わせて4本`() {
        val party = partyOf(member(Material.PLANT, 3, Personality.DEVOUT))

        assertEquals(
            Arrows(4, 0, listOf(ArrowSource("木曜の祭壇", 1, 0), ArrowSource("信心深い・木祭壇", 3, 0))),
            arrowsFor(0, party, snapshot(day = DayOfWeek.THURSDAY)),
        )
    }

    // ---- 性格の条件 ----

    @Test fun `昼行性は昼だけ+2、夕方はどの時間帯の性格も発動しない`() {
        val diurnal = partyOf(member(Material.WATER, 3, Personality.DIURNAL))
        val nocturnal = partyOf(member(Material.WATER, 3, Personality.NOCTURNAL))

        assertEquals(2, arrowsFor(0, diurnal, snapshot(timeBand = TimeBand.DAY)).quell)
        assertEquals(0, arrowsFor(0, diurnal, snapshot(timeBand = TimeBand.EVENING)).quell)
        assertEquals(0, arrowsFor(0, nocturnal, snapshot(timeBand = TimeBand.EVENING)).quell)
    }

    @Test fun `のんびり屋は土日に+3`() {
        val party = partyOf(member(Material.WATER, 3, Personality.RELAXED))

        assertEquals(3, arrowsFor(0, party, snapshot(day = DayOfWeek.SATURDAY)).quell)
        assertEquals(3, arrowsFor(0, party, snapshot(day = DayOfWeek.SUNDAY)).quell)
        assertEquals(0, arrowsFor(0, party, snapshot(day = DayOfWeek.FRIDAY)).quell)
    }

    @Test fun `月夜好きは満月のころ（月齢13以上17未満）に+3`() {
        val party = partyOf(member(Material.WATER, 3, Personality.MOON_LOVING))

        assertEquals(listOf(ArrowSource("月夜", 3, 0)), arrowsFor(0, party, snapshot(date = fullMoonDay)).sources)
        assertEquals(0, arrowsFor(0, party, neutralSnapshot).quell)
    }

    @Test fun `ためこみ屋は魔力+1、欲張りは魔力+3と引き換えに鎮め−1`() {
        val hoarder = partyOf(member(Material.WATER, 3, Personality.HOARDER))
        val greedy = partyOf(member(Material.WATER, 3, Personality.GREEDY))

        assertEquals(Arrows(0, 1, listOf(ArrowSource("ためこみ屋", 0, 1))), arrowsFor(0, hoarder, neutralSnapshot))
        assertEquals(Arrows(-1, 3, listOf(ArrowSource("欲張り", -1, 3))), arrowsFor(0, greedy, neutralSnapshot))
    }

    // ---- 先導役の隊長 ----

    @Test fun `先導役が隊長なら、隊長自身を含む全員に+1`() {
        val party = partyOf(
            member(Material.WATER, 3, Personality.LEADER),
            member(Material.STONE, 3, Personality.CALM),
        )

        assertEquals(Arrows(1, 0, listOf(ArrowSource("隊長の先導", 1, 0))), arrowsFor(0, party, neutralSnapshot))
        assertEquals(
            Arrows(2, 0, listOf(ArrowSource("穏やか", 1, 0), ArrowSource("隊長の先導", 1, 0))),
            arrowsFor(1, party, neutralSnapshot),
        )
    }

    @Test fun `先導役が隊長でなければ何も起きない`() {
        val party = partyOf(
            member(Material.WATER, 3, Personality.CALM),
            member(Material.STONE, 3, Personality.LEADER),
        )

        assertEquals(Arrows(1, 0, listOf(ArrowSource("穏やか", 1, 0))), arrowsFor(0, party, neutralSnapshot))
        assertEquals(Arrows(0, 0, emptyList()), arrowsFor(1, party, neutralSnapshot))
    }

    // ---- 境界 ----

    @Test fun `最悪は雨の日の欲張りな火の住人で、鎮めの矢印−2`() {
        val party = partyOf(member(Material.FIRE, 1, Personality.GREEDY))

        assertEquals(-2, arrowsFor(0, party, snapshot(weather = WeatherKind.RAIN)).quell)
    }

    // ---- 異常系 ----

    @Test fun `編成にいない枠を指定すると例外`() {
        val party = partyOf(member(Material.WATER, 3, Personality.CALM))

        assertThrows(IllegalArgumentException::class.java) { arrowsFor(1, party, neutralSnapshot) }
    }
}
