package dev.seirei.core.domain

import dev.seirei.core.model.ContextSnapshot
import dev.seirei.core.model.TimeBand
import dev.seirei.core.model.WeatherKind
import dev.seirei.core.testing.aSession
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 点火 Ignite（設計書01 §5）。:core:domain を読むときの入口。
 *
 * ガードは永続状態から `derivePhase` で評価し（原則D）、IDLE のときだけ新しい探索を始める。
 * 点火時に日付・時間帯・天気をスナップショットとして固定し、以後は変えない（設計書02 原則E）。
 *
 * 下位層の詳細はここでは扱わない：フェーズの導出は DerivePhaseTest、時間帯の境界は WeatherTest（:core:model）。
 * 並行性などの保証は guarantee/IgniteGuaranteeTest。
 */
class IgniteUseCaseTest {

    // ---- 代表例 ----

    @Test fun `IDLEなら点火し、探索は120分後に終わる`() = runTest {
        val now = Instant.parse("2026-10-17T00:00:00Z")
        val harness = IgniteHarness(now = now)

        assertEquals(IgniteResult.IGNITED, harness.ignite())
        assertEquals(now, harness.saved.startedAt)
        assertEquals(now.plus(Duration.ofMinutes(120)), harness.saved.endAt)
    }

    // ---- 点火時に決めて保存するもの ----

    @Test fun `点火した時点の日付・時間帯・天気をスナップショットに固定する`() = runTest {
        // UTC 15:00 は東京の翌日 0:00。日付も時間帯も端末のゾーンで決める
        val harness = IgniteHarness(now = Instant.parse("2026-10-16T15:00:00Z"), weather = WeatherKind.RAIN)

        harness.ignite()

        assertEquals(ContextSnapshot(LocalDate.of(2026, 10, 17), TimeBand.NIGHT, WeatherKind.RAIN), harness.saved.snapshot)
    }

    @Test fun `前回の編成をそのまま引き継ぐ`() = runTest {
        val harness = IgniteHarness(previousPartyIds = listOf(11L, 22L))

        harness.ignite()

        assertEquals(listOf(11L, 22L), harness.saved.partyIds)
    }

    @Test fun `シードは点火のたびに採番する`() = runTest {
        val harness = IgniteHarness(seed = 987654321L)

        harness.ignite()

        assertEquals(987654321L, harness.saved.seed)
    }

    @Test fun `初めての点火ではIDは1`() = runTest {
        val harness = IgniteHarness(current = null)

        harness.ignite()

        assertEquals(1L, harness.saved.id)
    }

    @Test fun `回収済みの探索のあとに点火すると、IDは前回の次になる`() = runTest {
        val harness = IgniteHarness(current = aSession(id = 41L, collected = true))

        assertEquals(IgniteResult.IGNITED, harness.ignite())
        assertEquals(42L, harness.saved.id)
    }

    @Test fun `天気が未取得でも点火でき、天気はUNKNOWNで固定する`() = runTest {
        val harness = IgniteHarness(weather = WeatherKind.UNKNOWN)

        assertEquals(IgniteResult.IGNITED, harness.ignite())
        assertEquals(WeatherKind.UNKNOWN, harness.saved.snapshot.weather)
    }

    // ---- ガード不成立：黙って何もしない（設計書01 §5） ----

    @Test fun `探索中に点火しても何も変わらない`() = runTest {
        val now = Instant.parse("2026-10-17T00:00:00Z")
        val exploring = aSession(startedAt = now.minusSeconds(60), endAt = now.plusSeconds(60))
        val harness = IgniteHarness(now = now, current = exploring)

        assertEquals(IgniteResult.NO_OP, harness.ignite())
        assertEquals(exploring, harness.saved)
    }

    @Test fun `帰還済みで未回収のときに点火しても何も変わらない`() = runTest {
        val now = Instant.parse("2026-10-17T00:00:00Z")
        val returned = aSession(endAt = now.minusNanos(1), collected = false)
        val harness = IgniteHarness(now = now, current = returned)

        assertEquals(IgniteResult.NO_OP, harness.ignite())
        assertEquals(returned, harness.saved)
    }
}
