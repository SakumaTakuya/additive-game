package dev.seirei.core.domain

import dev.seirei.core.model.Clock
import dev.seirei.core.model.ContextSnapshot
import dev.seirei.core.model.ExplorationSession
import dev.seirei.core.model.SessionPhase
import dev.seirei.core.model.WeatherKind
import dev.seirei.core.model.derivePhase
import dev.seirei.core.model.timeBandOf
import dev.seirei.core.roll.RollConstants
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** Read/write port for the latest persisted session (design 03 §4.2). */
interface SessionStore {
    suspend fun loadCurrent(): ExplorationSession?

    suspend fun save(session: ExplorationSession)
}

/** Port for the ordered resident IDs used by the previous party preset. */
fun interface PartyPresetStore {
    suspend fun loadPreviousPartyIds(): List<Long>
}

/** Reads the effective, already persisted weather; this port must not fetch the network. */
fun interface WeatherReader {
    suspend fun effectiveWeather(now: Instant): WeatherKind
}

/** Supplies a seed for a newly ignited session. */
fun interface SeedSource {
    fun nextSeed(): Long
}

enum class IgniteResult {
    IGNITED,
    NO_OP,
}

/**
 * Starts a session only when the persisted session currently derives to IDLE.
 * Context values are frozen from this invocation's single clock reading.
 */
class IgniteUseCase(
    private val clock: Clock,
    private val zone: ZoneId,
    private val constants: RollConstants,
    private val sessionStore: SessionStore,
    private val partyPresetStore: PartyPresetStore,
    private val weatherReader: WeatherReader,
    private val seedSource: SeedSource,
) {
    suspend fun invoke(): IgniteResult {
        val now = clock.now()
        val previous = sessionStore.loadCurrent()
        if (derivePhase(previous, now) != SessionPhase.IDLE) {
            return IgniteResult.NO_OP
        }

        val localDateTime = now.atZone(zone)
        val session = ExplorationSession(
            id = previous?.id?.plus(1L) ?: 1L,
            startedAt = now,
            endAt = now.plus(Duration.ofMinutes(constants.expeditionMinutes.toLong())),
            seed = seedSource.nextSeed(),
            partyIds = partyPresetStore.loadPreviousPartyIds(),
            snapshot = ContextSnapshot(
                date = localDateTime.toLocalDate(),
                timeBand = timeBandOf(localDateTime.toLocalTime()),
                weather = weatherReader.effectiveWeather(now),
            ),
            collected = false,
        )
        sessionStore.save(session)
        return IgniteResult.IGNITED
    }
}
