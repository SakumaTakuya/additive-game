package dev.seirei.core.testing

import dev.seirei.core.model.ColorTier
import dev.seirei.core.model.ContextSnapshot
import dev.seirei.core.model.ExplorationSession
import dev.seirei.core.model.Resident
import dev.seirei.core.model.TimeBand
import dev.seirei.core.model.WeatherKind
import java.time.Instant
import java.time.LocalDate

fun aResident(
    id: Long = 1L,
    motifId: Int = 1,
    star: Int = 3,
    personalityId: Int = 0,
    colorTier: ColorTier = ColorTier.NORMAL,
    acquiredAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    sourceSessionId: Long = 1L,
    locked: Boolean = false,
    nickname: String? = null,
): Resident = Resident(
    id = id,
    motifId = motifId,
    star = star,
    personalityId = personalityId,
    colorTier = colorTier,
    acquiredAt = acquiredAt,
    sourceSessionId = sourceSessionId,
    locked = locked,
    nickname = nickname,
)

fun aSession(
    id: Long = 1L,
    startedAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    endAt: Instant = startedAt.plusSeconds(7_200),
    seed: Long = 1L,
    partyIds: List<Long> = emptyList(),
    snapshot: ContextSnapshot = aSnapshot(),
    collected: Boolean = false,
): ExplorationSession = ExplorationSession(
    id = id,
    startedAt = startedAt,
    endAt = endAt,
    seed = seed,
    partyIds = partyIds,
    snapshot = snapshot,
    collected = collected,
)

fun aSnapshot(
    date: LocalDate = LocalDate.of(2026, 1, 1),
    timeBand: TimeBand = TimeBand.DAY,
    weather: WeatherKind = WeatherKind.UNKNOWN,
): ContextSnapshot = ContextSnapshot(
    date = date,
    timeBand = timeBand,
    weather = weather,
)
