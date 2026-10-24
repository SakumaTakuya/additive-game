package dev.seirei.core.domain.guarantee

import dev.seirei.core.domain.IgniteHarness
import dev.seirei.core.domain.IgniteResult
import dev.seirei.core.domain.RacingSessionStore
import dev.seirei.core.testing.aSession
import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 点火の保証。振る舞いは IgniteUseCaseTest を読むこと。
 * ここでは、ガード不成立時に副作用がないこと（設計書01 §5）と、同時に呼ばれても二重に始まらないこと（原則D）を確かめる。
 */
class IgniteGuaranteeTest {
    private val now = Instant.parse("2026-10-17T00:00:00Z")

    @Test fun `ガード不成立なら書き込まず、編成・天気・シードも読まない`() = runTest {
        val exploring = aSession(startedAt = now.minusSeconds(60), endAt = now.plusSeconds(60))
        val returned = aSession(endAt = now.minusNanos(1), collected = false)

        listOf(exploring, returned).forEach { current ->
            val harness = IgniteHarness(now = now, current = current)

            harness.ignite()

            assertEquals(0, harness.sessionStore.writes.size)
            assertEquals(0, harness.partyPresetStore.reads)
            assertEquals(0, harness.weatherReader.reads)
            assertEquals(0, harness.seedSource.reads)
        }
    }

    @Test fun `点火の書き込みは1回だけ`() = runTest {
        val harness = IgniteHarness(now = now)

        harness.ignite()

        assertEquals(1, harness.sessionStore.writes.size)
    }

    @Test fun `同時に2回点火しても、始まる探索は1つだけ`() = runTest {
        val harness = IgniteHarness(now = now, sessionStore = RacingSessionStore())

        val results = listOf(async { harness.ignite() }, async { harness.ignite() }).awaitAll()

        assertEquals(1, results.count { it == IgniteResult.IGNITED })
        assertEquals(1, results.count { it == IgniteResult.NO_OP })
        assertEquals(1, harness.sessionStore.writes.size)
    }
}
