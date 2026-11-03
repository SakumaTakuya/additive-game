package dev.seirei.core.data

import dev.seirei.core.database.ManaEntity
import dev.seirei.core.database.ResidentEntity
import dev.seirei.core.database.SeireiDatabase
import dev.seirei.core.database.SessionEntity
import dev.seirei.core.domain.ReturnTarget
import dev.seirei.core.model.ColorTier
import dev.seirei.core.model.TimeBand
import dev.seirei.core.model.WeatherKind
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 魔力に還すリポジトリ実装（設計書03 §4.4・§3）の仕様テスト。
 * 還元の書き込みとプリセット（セッション行 partyIds）の整合が、
 * リポジトリ層で取られることを確かめる。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReturnToManaRepositoryTest {

    @Test
    fun `魔力に還すと対象の住人が消えて魔力が増える`() = runBlocking {
        val database = createInMemoryDatabase()
        try {
            val repository = ReturnToManaRepository(database)
            seedMana(database, balance = 100)
            val partyMember = insertResident(database, motifId = 1)
            val target = insertResident(database, motifId = 2)
            insertSession(database, partyIds = listOf(partyMember))

            val refunded = repository.returnToMana(listOf(targetOf(database, target, refund = 10)))

            assertEquals(10, refunded)
            assertNull(database.residentDao().getById(target))
            assertNotNull(database.residentDao().getById(partyMember))
            assertEquals(110, database.manaDao().get()?.balance)
        } finally {
            database.close()
        }
    }

    @Test
    fun `魔力に還すとプリセットに残った存在しない住人のIDが除去される`() = runBlocking {
        val database = createInMemoryDatabase()
        try {
            val repository = ReturnToManaRepository(database)
            val sessions = SessionRepository(database)
            seedMana(database, balance = 0)
            val kept = insertResident(database, motifId = 1)
            val target = insertResident(database, motifId = 2)
            // プリセットに、既に存在しない住人の ID（999）が残っている状態。
            insertSession(database, partyIds = listOf(kept, 999L))

            repository.returnToMana(listOf(targetOf(database, target, refund = 10)))

            assertEquals(listOf(kept), sessions.loadPreviousPartyIds())
            assertNotNull(database.residentDao().getById(kept))
        } finally {
            database.close()
        }
    }

    @Test
    fun `保護された住人は還元されず残る`() = runBlocking {
        val database = createInMemoryDatabase()
        try {
            val repository = ReturnToManaRepository(database)
            seedMana(database, balance = 100)
            val locked = insertResident(database, motifId = 1, locked = true)

            val refunded = repository.returnToMana(listOf(targetOf(database, locked, refund = 10)))

            assertEquals(0, refunded)
            assertNotNull(database.residentDao().getById(locked))
            assertEquals(100, database.manaDao().get()?.balance)
        } finally {
            database.close()
        }
    }

    private suspend fun targetOf(database: SeireiDatabase, residentId: Long, refund: Int): ReturnTarget =
        ReturnTarget(
            resident = checkNotNull(database.residentDao().getById(residentId)?.toDomain()),
            refund = refund,
        )

    private suspend fun seedMana(database: SeireiDatabase, balance: Int) {
        database.manaDao().upsert(ManaEntity(balance = balance, lifetimeEarned = balance))
    }

    private suspend fun insertResident(
        database: SeireiDatabase,
        motifId: Int,
        locked: Boolean = false,
    ): Long =
        database.residentDao().insert(
            ResidentEntity(
                motifId = motifId,
                star = 3,
                personalityId = 0,
                colorTier = ColorTier.NORMAL,
                acquiredAt = Instant.parse("2026-10-25T00:00:00Z"),
                sourceSessionId = 1L,
                locked = locked,
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
