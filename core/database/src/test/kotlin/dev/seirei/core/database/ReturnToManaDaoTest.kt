package dev.seirei.core.database

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
 * 魔力に還すトランザクション（設計書03 §4.4）。
 * locked と編成中の個体は、対象として渡しても削除されず還元にも数えない。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReturnToManaDaoTest {
    @Test
    fun lockedAndPartyMembersAreNotDeleted() = runBlocking {
        val database = createInMemoryDatabase()
        try {
            seedMana(database, balance = 100)
            val freeId = database.residentDao().insert(aResident(motifId = 1))
            val lockedId = database.residentDao().insert(aResident(motifId = 2, locked = true))
            val partyId = database.residentDao().insert(aResident(motifId = 3))
            seedSession(database, partyIds = listOf(partyId))

            val refunded = database.returnToManaDao().returnToMana(
                listOf(
                    ReturnTargetRow(freeId, refund = 10),
                    ReturnTargetRow(lockedId, refund = 20),
                    ReturnTargetRow(partyId, refund = 40),
                ),
            )

            assertEquals(10, refunded)
            assertNull(database.residentDao().getById(freeId))
            assertNotNull(database.residentDao().getById(lockedId))
            assertNotNull(database.residentDao().getById(partyId))
            assertEquals(110, database.manaDao().get()?.balance)
        } finally {
            database.close()
        }
    }

    @Test
    fun residentLockedAfterPreviewIsSkippedAtWriteTime() = runBlocking {
        val database = createInMemoryDatabase()
        try {
            seedMana(database, balance = 0)
            val id = database.residentDao().insert(aResident(motifId = 1))
            database.residentDao().updateLocked(id, true)

            val refunded = database.returnToManaDao().returnToMana(listOf(ReturnTargetRow(id, refund = 10)))

            assertEquals(0, refunded)
            assertNotNull(database.residentDao().getById(id))
            assertEquals(0, database.manaDao().get()?.balance)
        } finally {
            database.close()
        }
    }

    private suspend fun seedMana(database: SeireiDatabase, balance: Int) {
        database.manaDao().upsert(ManaEntity(balance = balance, lifetimeEarned = balance))
    }

    private suspend fun seedSession(database: SeireiDatabase, partyIds: List<Long>) {
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
                collected = false,
            ),
        )
    }

    private fun aResident(motifId: Int, locked: Boolean = false) = ResidentEntity(
        motifId = motifId,
        star = 3,
        personalityId = 0,
        colorTier = ColorTier.NORMAL,
        acquiredAt = Instant.parse("2026-10-25T00:00:00Z"),
        sourceSessionId = 1L,
        locked = locked,
    )
}
