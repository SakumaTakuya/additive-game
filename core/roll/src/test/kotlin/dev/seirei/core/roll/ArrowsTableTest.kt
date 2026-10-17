package dev.seirei.core.roll

import dev.seirei.core.model.ContextSnapshot
import dev.seirei.core.model.Material
import dev.seirei.core.model.Party
import dev.seirei.core.model.PartyMember
import dev.seirei.core.model.Personality
import dev.seirei.core.model.TimeBand
import dev.seirei.core.model.WeatherGroup
import dev.seirei.core.model.WeatherKind
import dev.seirei.core.model.moonAge
import dev.seirei.core.model.shrineMaterialOf
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ArrowsTableTest {
    /**
     * This table deliberately states the design rules independently of arrowsFor().
     * It covers each personality, material, weather, time band, weekday, moon condition,
     * and leader presence; adding an enum value or changing a rule requires a conscious update.
     */
    @Test
    fun arrowsMatchIndependentRuleTableAcrossAllInputs() {
        val dates = datesCoveringWeekdaysAndMoonConditions()
        var cases = 0

        for (personality in Personality.entries) {
            for (material in Material.entries) {
                for (weather in WeatherKind.entries) {
                    for (timeBand in TimeBand.entries) {
                        for (date in dates) {
                            for (hasLeader in listOf(false, true)) {
                                val subject = PartyMember(knownMotifId(material), 3, personality.id)
                                val leader = if (hasLeader) PartyMember(1, 3, Personality.LEADER.id) else null
                                val members = if (leader == null) listOf(subject) else listOf(leader, subject)
                                val slot = if (leader == null) 0 else 1
                                val party = Party(members)
                                val snapshot = ContextSnapshot(date, timeBand, weather)

                                assertEquals(
                                    "personality=$personality material=$material weather=$weather time=$timeBand date=$date leader=$hasLeader",
                                    expected(personality, material, date, weatherRule(weather), timeBand, hasLeader),
                                    arrowsFor(slot, party, snapshot),
                                )
                                cases++
                            }
                        }
                    }
                }
            }
        }
        assertEquals(12 * 7 * 5 * 3 * 14 * 2, cases)
    }

    @Test
    fun representativeRainyThursdaySourcesAreExposed() {
        val date = LocalDate.of(2026, 10, 15) // Thursday
        val snapshot = ContextSnapshot(date, TimeBand.EVENING, WeatherKind.RAIN)

        assertEquals(Arrows(2, 0, listOf(
            ArrowSource("雨", 1, 0),
            ArrowSource("隊長の先導", 1, 0),
        )), arrowsFor(1, Party(listOf(
            PartyMember(1, 3, Personality.LEADER.id), PartyMember(3, 3, Personality.DIURNAL.id),
        )), snapshot))
        assertEquals(Arrows(0, 0, listOf(
            ArrowSource("雨", -1, 0),
            ArrowSource("隊長の先導", 1, 0),
        )), arrowsFor(1, Party(listOf(PartyMember(1, 3, Personality.LEADER.id), PartyMember(6, 3, Personality.DIURNAL.id))), snapshot))
        assertEquals(Arrows(2, 0, listOf(
            ArrowSource("木曜の祭壇", 1, 0),
            ArrowSource("隊長の先導", 1, 0),
        )), arrowsFor(1, Party(listOf(PartyMember(1, 3, Personality.LEADER.id), PartyMember(10, 3, Personality.DIURNAL.id))), snapshot))
    }

    @Test
    fun worstCaseRainOnFireWithGreedyAndNoLeaderIsMinusTwo() {
        val party = Party(listOf(PartyMember(6, 1, Personality.GREEDY.id)))
        val snapshot = ContextSnapshot(LocalDate.of(2026, 10, 15), TimeBand.EVENING, WeatherKind.RAIN)

        assertEquals(-2, arrowsFor(0, party, snapshot).quell)
    }

    private fun expected(
        personality: Personality,
        material: Material,
        date: LocalDate,
        weather: WeatherGroup,
        timeBand: TimeBand,
        hasLeader: Boolean,
    ): Arrows {
        val sources = mutableListOf<ArrowSource>()
        val (goodWeather, badWeather, shrineDay) = materialRule(material)
        if (weather != WeatherGroup.NONE && weather == goodWeather) {
            sources += ArrowSource(weatherLabel(weather), 1, 0)
        }
        if (weather != WeatherGroup.NONE && weather == badWeather) {
            sources += ArrowSource(weatherLabel(weather), -1, 0)
        }
        if (date.dayOfWeek == shrineDay) sources += ArrowSource("${weekdayLabel(date.dayOfWeek)}曜の祭壇", 1, 0)

        val (isActive, quell, mana) = when (personality) {
            Personality.CALM -> Triple(true, 1, 0)
            Personality.SUN_LOVING -> Triple(weather == WeatherGroup.SUN, 2, 0)
            Personality.CLOUD_LOVING -> Triple(weather == WeatherGroup.CLOUD, 2, 0)
            Personality.RAIN_LOVING -> Triple(weather == WeatherGroup.WET, 2, 0)
            Personality.DIURNAL -> Triple(timeBand == TimeBand.DAY, 2, 0)
            Personality.NOCTURNAL -> Triple(timeBand == TimeBand.NIGHT, 2, 0)
            Personality.RELAXED -> Triple(date.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), 3, 0)
            Personality.MOON_LOVING -> Triple(referenceMoonAge(date) in 13.0..<17.0, 3, 0)
            Personality.DEVOUT -> Triple(date.dayOfWeek == shrineDay, 3, 0)
            Personality.LEADER -> Triple(false, 0, 0)
            Personality.HOARDER -> Triple(true, 0, 1)
            Personality.GREEDY -> Triple(true, -1, 3)
        }
        if (isActive) {
            sources += ArrowSource(personalityLabel(personality, weather, date), quell, mana)
        }
        if (hasLeader || personality == Personality.LEADER) sources += ArrowSource("隊長の先導", 1, 0)
        return Arrows(sources.sumOf { it.quell }, sources.sumOf { it.mana }, sources)
    }

    private fun datesCoveringWeekdaysAndMoonConditions(): List<LocalDate> {
        val found = mutableMapOf<Pair<DayOfWeek, Boolean>, LocalDate>()
        var date = LocalDate.of(2026, 1, 1)
        while (found.size < 14) {
            val isFullMoonWindow = referenceMoonAge(date) in 13.0..<17.0
            found.putIfAbsent(date.dayOfWeek to isFullMoonWindow, date)
            date = date.plusDays(1)
        }
        return DayOfWeek.entries.flatMap { day -> listOf(false, true).map { found.getValue(day to it) } }
    }

    private fun referenceMoonAge(date: LocalDate): Double {
        val julianDay = date.toEpochDay() + 2440588.0
        val synodicMonth = 29.530588853
        return ((julianDay - 2451550.26) % synodicMonth + synodicMonth) % synodicMonth
    }

    private fun knownMotifId(material: Material): Int = when (material) {
        Material.WATER -> 1
        Material.EARTHENWARE -> 3
        Material.STONE -> 5
        Material.BONE -> 4
        Material.METAL -> 9
        Material.FIRE -> 6
        Material.PLANT -> 10
    }

    private fun materialRule(material: Material): Triple<WeatherGroup, WeatherGroup?, DayOfWeek> = when (material) {
        Material.WATER -> Triple(WeatherGroup.WET, null, DayOfWeek.WEDNESDAY)
        Material.EARTHENWARE -> Triple(WeatherGroup.WET, null, DayOfWeek.SATURDAY)
        Material.STONE -> Triple(WeatherGroup.CLOUD, null, DayOfWeek.SUNDAY)
        Material.BONE -> Triple(WeatherGroup.CLOUD, null, DayOfWeek.MONDAY)
        Material.METAL -> Triple(WeatherGroup.SUN, null, DayOfWeek.FRIDAY)
        Material.FIRE -> Triple(WeatherGroup.SUN, WeatherGroup.WET, DayOfWeek.TUESDAY)
        Material.PLANT -> Triple(WeatherGroup.SUN, null, DayOfWeek.THURSDAY)
    }

    private fun weatherLabel(weather: WeatherGroup): String = when (weather) {
        WeatherGroup.SUN -> "晴れ"
        WeatherGroup.CLOUD -> "曇り"
        WeatherGroup.WET -> "雨"
        WeatherGroup.NONE -> error("NONE has no active weather label")
    }

    private fun weatherRule(weather: WeatherKind): WeatherGroup = when (weather) {
        WeatherKind.SUNNY -> WeatherGroup.SUN
        WeatherKind.CLOUDY -> WeatherGroup.CLOUD
        WeatherKind.RAIN, WeatherKind.SNOW -> WeatherGroup.WET
        WeatherKind.UNKNOWN -> WeatherGroup.NONE
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

    private fun personalityLabel(personality: Personality, weather: WeatherGroup, date: LocalDate): String = when (personality) {
        Personality.CALM -> "穏やか"
        Personality.SUN_LOVING -> "晴れ好き・晴れ"
        Personality.CLOUD_LOVING -> "曇り好き・曇り"
        Personality.RAIN_LOVING -> "雨好き・雨"
        Personality.DIURNAL -> "昼行性・昼"
        Personality.NOCTURNAL -> "夜行性・夜"
        Personality.RELAXED -> "週末"
        Personality.MOON_LOVING -> "月夜"
        Personality.DEVOUT -> "信心深い・${weekdayLabel(date.dayOfWeek)}祭壇"
        Personality.LEADER -> "先導役"
        Personality.HOARDER -> "ためこみ屋"
        Personality.GREEDY -> "欲張り"
    }
}
