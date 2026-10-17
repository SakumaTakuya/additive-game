package dev.seirei.core.roll

import dev.seirei.core.model.Condition
import dev.seirei.core.model.ContextSnapshot
import dev.seirei.core.model.Material
import dev.seirei.core.model.Party
import dev.seirei.core.model.Personality
import dev.seirei.core.model.WeatherGroup
import dev.seirei.core.model.group
import dev.seirei.core.model.moonAge
import dev.seirei.core.model.shrineMaterialOf
import dev.seirei.core.model.Motif
import java.time.DayOfWeek

/** One explanatory contribution to a resident's modifiers (design 02 §5.3). */
data class ArrowSource(val label: String, val quell: Int, val mana: Int)

/** Net modifiers plus their user-visible reasons. */
data class Arrows(val quell: Int, val mana: Int, val sources: List<ArrowSource>)

/**
 * Evaluates the ignition-time rules for one member of an ordered party.
 * Slot zero is the leader; all weather, time, date, and moon checks use the fixed snapshot.
 */
fun arrowsFor(slot: Int, party: Party, snapshot: ContextSnapshot): Arrows {
    require(slot in party.members.indices) { "slot $slot is outside party of size ${party.members.size}" }

    val member = party.members[slot]
    val motif = Motif.ALL.singleOrNull { it.id == member.motifId }
        ?: throw IllegalArgumentException("Unknown motif id: ${member.motifId}")
    val personality = Personality.entries.getOrNull(member.personalityId)
        ?: throw IllegalArgumentException("Unknown personality id: ${member.personalityId}")
    val weather = snapshot.weather.group
    val shrine = shrineMaterialOf(snapshot.date)
    val sources = mutableListOf<ArrowSource>()

    if (weather != WeatherGroup.NONE && weather == motif.material.goodWeather) {
        sources += ArrowSource(weatherLabel(weather), 1, 0)
    } else if (weather != WeatherGroup.NONE && weather == motif.material.badWeather) {
        sources += ArrowSource(weatherLabel(weather), -1, 0)
    }
    if (shrine == motif.material) {
        sources += ArrowSource("${weekdayLabel(snapshot.date.dayOfWeek)}曜の祭壇", 1, 0)
    }

    if (conditionMatches(personality.condition, motif.material, slot, snapshot, weather)) {
        when (personality) {
            Personality.LEADER -> Unit // Its effect is applied once to every party member below.
            else -> sources += ArrowSource(
                personalityLabel(personality, weather, snapshot),
                personality.quellArrows,
                personality.manaArrows,
            )
        }
    }

    if (party.members.firstOrNull()?.personalityId == Personality.LEADER.id) {
        sources += ArrowSource("隊長の先導", 1, 0)
    }

    return Arrows(sources.sumOf(ArrowSource::quell), sources.sumOf(ArrowSource::mana), sources.toList())
}

private fun conditionMatches(
    condition: Condition,
    material: Material,
    slot: Int,
    snapshot: ContextSnapshot,
    weather: WeatherGroup,
): Boolean = when (condition) {
    Condition.Always -> true
    is Condition.WeatherIs -> weather != WeatherGroup.NONE && weather == condition.group
    is Condition.TimeBandIs -> snapshot.timeBand == condition.band
    Condition.Weekend -> snapshot.date.dayOfWeek == DayOfWeek.SATURDAY || snapshot.date.dayOfWeek == DayOfWeek.SUNDAY
    Condition.FullMoon -> moonAge(snapshot.date) >= 13.0 && moonAge(snapshot.date) < 17.0
    Condition.ShrineMatch -> shrineMaterialOf(snapshot.date) == material
    Condition.IsLeader -> slot == 0
}

private fun weatherLabel(weather: WeatherGroup): String = when (weather) {
    WeatherGroup.SUN -> "晴れ"
    WeatherGroup.CLOUD -> "曇り"
    WeatherGroup.WET -> "雨"
    WeatherGroup.NONE -> error("Neutral weather has no active modifier")
}

private fun personalityLabel(
    personality: Personality,
    weather: WeatherGroup,
    snapshot: ContextSnapshot,
): String = when (personality) {
    Personality.CALM -> "穏やか"
    Personality.SUN_LOVING -> "晴れ好き・${weatherLabel(weather)}"
    Personality.CLOUD_LOVING -> "曇り好き・${weatherLabel(weather)}"
    Personality.RAIN_LOVING -> "雨好き・${weatherLabel(weather)}"
    Personality.DIURNAL -> "昼行性・昼"
    Personality.NOCTURNAL -> "夜行性・夜"
    Personality.RELAXED -> "週末"
    Personality.MOON_LOVING -> "月夜"
    Personality.DEVOUT -> "信心深い・${weekdayLabel(snapshot.date.dayOfWeek)}祭壇"
    Personality.LEADER -> "先導役"
    Personality.HOARDER -> "ためこみ屋"
    Personality.GREEDY -> "欲張り"
}

private fun weekdayLabel(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "月"
    DayOfWeek.TUESDAY -> "火"
    DayOfWeek.WEDNESDAY -> "水"
    DayOfWeek.THURSDAY -> "木"
    DayOfWeek.FRIDAY -> "金"
    DayOfWeek.SATURDAY -> "土"
    DayOfWeek.SUNDAY -> "日"
}
