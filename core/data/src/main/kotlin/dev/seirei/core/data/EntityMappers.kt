package dev.seirei.core.data

import dev.seirei.core.database.ResidentEntity
import dev.seirei.core.database.SessionEntity
import dev.seirei.core.model.ContextSnapshot
import dev.seirei.core.model.ExplorationSession
import dev.seirei.core.model.Resident
import dev.seirei.core.roll.ResidentRoll
import java.time.Instant

internal fun SessionEntity.toDomain(): ExplorationSession = ExplorationSession(
    id = id,
    startedAt = startedAt,
    endAt = endAt,
    seed = seed,
    partyIds = partyIds,
    snapshot = ContextSnapshot(
        date = snapshotDate,
        timeBand = snapshotTimeBand,
        weather = snapshotWeather,
    ),
    collected = collected,
)

internal fun ExplorationSession.toEntity(): SessionEntity = SessionEntity(
    id = id,
    startedAt = startedAt,
    endAt = endAt,
    seed = seed,
    snapshotDate = snapshot.date,
    snapshotTimeBand = snapshot.timeBand,
    snapshotWeather = snapshot.weather,
    partyIds = partyIds,
    collected = collected,
)

internal fun ResidentEntity.toDomain(): Resident = Resident(
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

/** ロールの結果を住人行にする。acquiredAt は書き込み側（時計を持つ層）が決める。 */
internal fun ResidentRoll.toEntity(sourceSessionId: Long, acquiredAt: Instant): ResidentEntity =
    ResidentEntity(
        motifId = motifId,
        star = star,
        personalityId = personalityId,
        colorTier = color,
        acquiredAt = acquiredAt,
        sourceSessionId = sourceSessionId,
    )
