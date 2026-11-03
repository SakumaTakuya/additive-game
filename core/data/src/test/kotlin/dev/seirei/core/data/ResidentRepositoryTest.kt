package dev.seirei.core.data

import app.cash.turbine.test
import dev.seirei.core.database.ResidentEntity
import dev.seirei.core.database.SeireiDatabase
import dev.seirei.core.database.SessionEntity
import dev.seirei.core.model.ColorTier
import dev.seirei.core.model.TimeBand
import dev.seirei.core.model.WeatherKind
import java.time.Instant
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * リポジトリの購読クエリ（設計書03 §5）の仕様テスト。
 * 編成（セッション行の partyIds）を変えると、留守番組・編成・発見済みモチーフの
 * Flow がそれに追随して変わることを確かめる。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ResidentRepositoryTest {

    @Test
    fun `編成を変えると留守番組が即座に変わる`() = runBlocking {
        val database = createInMemoryDatabase()
        try {
            val residents = ResidentRepository(database)
            val sessions = SessionRepository(database)
            val a = insertResident(database, motifId = 1)
            val b = insertResident(database, motifId = 2)
            val c = insertResident(database, motifId = 3)
            insertSession(database, partyIds = listOf(a))

            residents.caretakersFlow.test(timeout = 10.seconds) {
                assertEquals(listOf(b, c), awaitItem().map { it.id })
                sessions.savePartyPreset(listOf(a, b))
                assertEquals(listOf(c), awaitItem().map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun `セッション行が無いときは全員が留守番組になる`() = runBlocking {
        val database = createInMemoryDatabase()
        try {
            val residents = ResidentRepository(database)
            val a = insertResident(database, motifId = 1)
            val b = insertResident(database, motifId = 2)

            assertEquals(listOf(a, b), residents.caretakersFlow.first().map { it.id })
        } finally {
            database.close()
        }
    }

    @Test
    fun `編成はプリセットの順序どおりに並び先頭が隊長になる`() = runBlocking {
        val database = createInMemoryDatabase()
        try {
            val residents = ResidentRepository(database)
            val a = insertResident(database, motifId = 1)
            val b = insertResident(database, motifId = 2)
            val c = insertResident(database, motifId = 3)
            insertSession(database, partyIds = listOf(c, a))

            val party = residents.partyFlow.first()
            assertEquals(listOf(c, a), party.map { it.id })
            assertEquals(c, party.first().id)
            assertEquals(listOf(b), residents.caretakersFlow.first().map { it.id })
        } finally {
            database.close()
        }
    }

    @Test
    fun `発見済みモチーフは住人のモチーフの重複なし集合になる`() = runBlocking {
        val database = createInMemoryDatabase()
        try {
            val residents = ResidentRepository(database)
            insertResident(database, motifId = 1)
            insertResident(database, motifId = 1)
            insertResident(database, motifId = 2)

            assertEquals(setOf(1, 2), residents.discoveredMotifsFlow.first())

            insertResident(database, motifId = 3)
            assertEquals(setOf(1, 2, 3), residents.discoveredMotifsFlow.first())
        } finally {
            database.close()
        }
    }

    @Test
    fun `セッション行が無いときプリセット保存は何もしない`() = runBlocking {
        val database = createInMemoryDatabase()
        try {
            val sessions = SessionRepository(database)
            val a = insertResident(database, motifId = 1)

            sessions.savePartyPreset(listOf(a))

            assertNull(sessions.sessionFlow.first())
            assertEquals(emptyList<Long>(), sessions.loadPreviousPartyIds())
        } finally {
            database.close()
        }
    }

    private suspend fun insertResident(database: SeireiDatabase, motifId: Int): Long =
        database.residentDao().insert(
            ResidentEntity(
                motifId = motifId,
                star = 3,
                personalityId = 0,
                colorTier = ColorTier.NORMAL,
                acquiredAt = Instant.parse("2026-10-25T00:00:00Z"),
                sourceSessionId = 1L,
            ),
        )

    private suspend fun insertSession(database: SeireiDatabase, partyIds: List<Long>) {
        database.sessionDao().upsert(
            SessionEntity(
                id = 1L,
                startedAt = Instant.parse("2026-10-25T00:00:00Z"),
                endAt = Instant.parse("2026-10-25T02:00:00Z"),
                seed = 42L,
                snapshotDate = LocalDate.of(2026, 10, 25),
                snapshotTimeBand = TimeBand.DAY,
                snapshotWeather = WeatherKind.UNKNOWN,
                partyIds = partyIds,
                collected = true,
            ),
        )
    }
}
