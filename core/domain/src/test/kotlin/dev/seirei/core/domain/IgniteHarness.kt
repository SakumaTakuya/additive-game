package dev.seirei.core.domain

import dev.seirei.core.model.ExplorationSession
import dev.seirei.core.model.WeatherKind
import dev.seirei.core.roll.RollConstants
import dev.seirei.core.testing.FakeClock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred

/**
 * IgniteUseCase を組み立てるテスト用の土台。テスト本文には「いつ・何が保存されていて・前回の編成と天気は何か」だけを書く。
 * 時刻のゾーンは Asia/Tokyo、定数は POC（探索120分）。
 */
internal class IgniteHarness(
    now: Instant = Instant.parse("2026-10-17T00:00:00Z"),
    current: ExplorationSession? = null,
    previousPartyIds: List<Long> = emptyList(),
    weather: WeatherKind = WeatherKind.UNKNOWN,
    seed: Long = 1L,
    val sessionStore: RecordingSessionStore = FakeSessionStore(current),
) {
    val clock = FakeClock(now)
    val partyPresetStore = FakePartyPresetStore(previousPartyIds)
    val weatherReader = FakeWeatherReader(weather)
    val seedSource = FakeSeedSource(seed)

    val useCase = IgniteUseCase(
        clock = clock,
        zone = ZoneId.of("Asia/Tokyo"),
        constants = RollConstants.POC,
        sessionStore = sessionStore,
        partyPresetStore = partyPresetStore,
        weatherReader = weatherReader,
        seedSource = seedSource,
    )

    suspend fun ignite(): IgniteResult = useCase.invoke()

    /** 点火後に保存されているセッション。 */
    val saved: ExplorationSession
        get() = requireNotNull(sessionStore.current) { "no session is saved" }
}

internal interface RecordingSessionStore : SessionStore {
    val current: ExplorationSession?
    val writes: List<ExplorationSession>
}

internal class FakeSessionStore(initial: ExplorationSession? = null) : RecordingSessionStore {
    override var current: ExplorationSession? = initial
        private set
    override val writes = mutableListOf<ExplorationSession>()

    override suspend fun loadCurrent(): ExplorationSession? = current

    override suspend fun replaceCurrentIfUnchanged(
        expectedCurrent: ExplorationSession?,
        replacement: ExplorationSession,
    ): Boolean {
        if (current != expectedCurrent) return false
        writes += replacement
        current = replacement
        return true
    }
}

/** 2つの呼び出しが両方とも読み終わるまで loadCurrent を返さない。読み込みと書き込みの間に割り込まれた状況を作る。 */
internal class RacingSessionStore : RecordingSessionStore {
    override var current: ExplorationSession? = null
        private set
    override val writes = mutableListOf<ExplorationSession>()
    private var loadCount = 0
    private val bothLoaded = CompletableDeferred<Unit>()

    override suspend fun loadCurrent(): ExplorationSession? {
        val snapshot = synchronized(this) {
            loadCount += 1
            if (loadCount == 2) bothLoaded.complete(Unit)
            current
        }
        bothLoaded.await()
        return snapshot
    }

    override suspend fun replaceCurrentIfUnchanged(
        expectedCurrent: ExplorationSession?,
        replacement: ExplorationSession,
    ): Boolean = synchronized(this) {
        if (current != expectedCurrent) return@synchronized false
        writes += replacement
        current = replacement
        true
    }
}

internal class FakePartyPresetStore(private val partyIds: List<Long>) : PartyPresetStore {
    var reads: Int = 0
        private set

    override suspend fun loadPreviousPartyIds(): List<Long> {
        reads += 1
        return partyIds
    }
}

internal class FakeWeatherReader(private val weather: WeatherKind) : WeatherReader {
    var reads: Int = 0
        private set

    override suspend fun effectiveWeather(now: Instant): WeatherKind {
        reads += 1
        return weather
    }
}

internal class FakeSeedSource(private val seed: Long) : SeedSource {
    var reads: Int = 0
        private set

    override fun nextSeed(): Long {
        reads += 1
        return seed
    }
}
