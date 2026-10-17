package dev.seirei.core.roll

import dev.seirei.core.model.ContextSnapshot
import dev.seirei.core.model.Party
import dev.seirei.core.model.PartyMember
import dev.seirei.core.model.Personality
import dev.seirei.core.model.TimeBand
import dev.seirei.core.model.WeatherKind
import java.time.LocalDate

internal val noModifierSnapshot = ContextSnapshot(
    date = LocalDate.of(2026, 1, 5), // Monday: shrine does not match motif 1 (water).
    timeBand = TimeBand.DAY,
    weather = WeatherKind.UNKNOWN,
)

internal fun noModifierParty(vararg stars: Int): Party = Party(
    stars.map { star ->
        PartyMember(
            motifId = 1,
            star = star,
            personalityId = Personality.NOCTURNAL.id,
        )
    },
)

internal val cappedParty = Party(
    listOf(
        PartyMember(motifId = 1, star = 5, personalityId = Personality.LEADER.id),
        PartyMember(motifId = 1, star = 5, personalityId = Personality.RELAXED.id),
        PartyMember(motifId = 1, star = 5, personalityId = Personality.RELAXED.id),
    ),
)

internal val weekendSnapshot = ContextSnapshot(
    date = LocalDate.of(2026, 1, 3), // Saturday, with unknown weather.
    timeBand = TimeBand.DAY,
    weather = WeatherKind.UNKNOWN,
)
