package dev.seirei.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalityTableTest {
    @Test
    fun entriesKeepPersistedIdsInDesignOrder() {
        // This locks the enum order because ordinal is the persisted personality ID.
        assertEquals(
            listOf("穏やか", "晴れ好き", "曇り好き", "雨好き", "昼行性", "夜行性", "のんびり屋", "月夜好き", "信心深い", "先導役", "ためこみ屋", "欲張り"),
            Personality.entries.map { it.displayName },
        )
        assertEquals((0..11).toList(), Personality.entries.map { it.id })
    }

    @Test
    fun abbreviationsAreUnique() {
        assertEquals(12, Personality.entries.map { it.abbreviation }.toSet().size)
    }

    @Test
    fun personalityTableMatchesDesign() {
        val expected = listOf(
            Expected("穏やか", "穏", "いつでも落ち着いて鎮められる", Condition.Always, 1, 0),
            Expected("晴れ好き", "晴", "晴れの日、鎮めがうまくいきやすい", Condition.WeatherIs(WeatherGroup.SUN), 2, 0),
            Expected("曇り好き", "曇", "曇りの日、鎮めがうまくいきやすい", Condition.WeatherIs(WeatherGroup.CLOUD), 2, 0),
            Expected("雨好き", "雨", "雨や雪の日、鎮めがうまくいきやすい", Condition.WeatherIs(WeatherGroup.WET), 2, 0),
            Expected("昼行性", "昼", "昼に灯すと、鎮めがうまくいきやすい", Condition.TimeBandIs(TimeBand.DAY), 2, 0),
            Expected("夜行性", "夜", "夜に灯すと、鎮めがうまくいきやすい", Condition.TimeBandIs(TimeBand.NIGHT), 2, 0),
            Expected("のんびり屋", "休", "週末は、とても調子がいい", Condition.Weekend, 3, 0),
            Expected("月夜好き", "月", "満月のころ、とても調子がいい", Condition.FullMoon, 3, 0),
            Expected("信心深い", "信", "自分の祭壇の日、とても調子がいい", Condition.ShrineMatch, 3, 0),
            Expected("先導役", "導", "隊長にすると、仲間みんなが鎮めやすくなる", Condition.IsLeader, 1, 0, EffectTarget.PARTY),
            Expected("ためこみ屋", "溜", "魔力を少し多く持ち帰る", Condition.Always, 0, 1),
            Expected("欲張り", "欲", "魔力をたくさん持ち帰るが、鎮めは苦手", Condition.Always, -1, 3),
        )

        assertEquals(expected, Personality.entries.map {
            Expected(it.displayName, it.abbreviation, it.description, it.condition, it.quellArrows, it.manaArrows, it.effectTarget)
        })
        assertTrue(Personality.entries.all { it.id in 0..11 })
    }

    private data class Expected(
        val displayName: String,
        val abbreviation: String,
        val description: String,
        val condition: Condition,
        val quellArrows: Int,
        val manaArrows: Int,
        val effectTarget: EffectTarget = EffectTarget.SELF,
    )
}
