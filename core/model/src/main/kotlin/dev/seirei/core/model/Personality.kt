package dev.seirei.core.model

/** Personality modifiers are data; condition evaluation lives in :core:roll (design 02 §5.3). */
enum class Personality(
    val displayName: String,
    val abbreviation: String,
    val description: String,
    val condition: Condition,
    val quellArrows: Int,
    val manaArrows: Int,
    val effectTarget: EffectTarget = EffectTarget.SELF,
) {
    CALM("穏やか", "穏", "いつでも落ち着いて鎮められる", Condition.Always, 1, 0),
    SUN_LOVING("晴れ好き", "晴", "晴れの日、鎮めがうまくいきやすい", Condition.WeatherIs(WeatherGroup.SUN), 2, 0),
    CLOUD_LOVING("曇り好き", "曇", "曇りの日、鎮めがうまくいきやすい", Condition.WeatherIs(WeatherGroup.CLOUD), 2, 0),
    RAIN_LOVING("雨好き", "雨", "雨や雪の日、鎮めがうまくいきやすい", Condition.WeatherIs(WeatherGroup.WET), 2, 0),
    DIURNAL("昼行性", "昼", "昼に灯すと、鎮めがうまくいきやすい", Condition.TimeBandIs(TimeBand.DAY), 2, 0),
    NOCTURNAL("夜行性", "夜", "夜に灯すと、鎮めがうまくいきやすい", Condition.TimeBandIs(TimeBand.NIGHT), 2, 0),
    RELAXED("のんびり屋", "休", "週末は、とても調子がいい", Condition.Weekend, 3, 0),
    MOON_LOVING("月夜好き", "月", "満月のころ、とても調子がいい", Condition.FullMoon, 3, 0),
    DEVOUT("信心深い", "信", "自分の祭壇の日、とても調子がいい", Condition.ShrineMatch, 3, 0),
    LEADER("先導役", "導", "隊長にすると、仲間みんなが鎮めやすくなる", Condition.IsLeader, 1, 0, EffectTarget.PARTY),
    HOARDER("ためこみ屋", "溜", "魔力を少し多く持ち帰る", Condition.Always, 0, 1),
    GREEDY("欲張り", "欲", "魔力をたくさん持ち帰るが、鎮めは苦手", Condition.Always, -1, 3),
    ;

    /** The ordinal is persisted as resident.personalityId; keep enum order stable. */
    val id: Int get() = ordinal
}

/** Conditions are interpreted against the ignition snapshot by the roll engine. */
sealed interface Condition {
    data object Always : Condition
    data class WeatherIs(val group: WeatherGroup) : Condition
    data class TimeBandIs(val band: TimeBand) : Condition
    data object Weekend : Condition
    data object FullMoon : Condition
    data object ShrineMatch : Condition
    data object IsLeader : Condition
}

/** Most personality effects apply to their owner; the leader bonus applies to every party member. */
enum class EffectTarget { SELF, PARTY }
