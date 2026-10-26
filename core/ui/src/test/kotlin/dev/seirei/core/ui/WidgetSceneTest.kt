package dev.seirei.core.ui

import dev.seirei.core.model.ContextSnapshot
import dev.seirei.core.model.TimeBand
import dev.seirei.core.model.WeatherKind
import dev.seirei.core.testing.aResident
import dev.seirei.core.testing.aSession
import dev.seirei.core.testing.aSnapshot
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Widget の場面導出（設計書04 §3.1・§3.2、設計書01 §6.2、DEC-12）。
 * 追加の永続状態ゼロの純関数：同じ入力・同じ時刻なら同じ場面になり、
 * 配置は1時間粒度で変わる。表示上限を超えた分は日替わりローテーション。
 */
class WidgetSceneTest {
    private val noon: Instant = Instant.parse("2026-10-25T12:00:00Z")
    private val today: ContextSnapshot = aSnapshot(date = LocalDate.of(2026, 10, 25))

    @Test
    fun `同じ入力と同じ時刻なら同じ場面になる`() {
        val residents = listOf(aResident(id = 1L), aResident(id = 2L), aResident(id = 3L))

        val first = deriveWidgetScene(null, residents, today, noon, WidgetSize.LARGE)
        val second = deriveWidgetScene(null, residents, today, noon, WidgetSize.LARGE)

        assertEquals(first, second)
        assertEquals(3, first.residents.size)
    }

    @Test
    fun `同じ1時間内では配置が変わらず、1時間進むと変わりうる`() {
        val residents = (1L..6L).map { aResident(id = it) }
        val base = deriveWidgetScene(null, residents, today, noon, WidgetSize.LARGE)

        val sameHour = deriveWidgetScene(null, residents, today, noon.plusSeconds(1_800), WidgetSize.LARGE)
        assertEquals(base, sameHour)

        val laterHours = (1L..24L).map { h ->
            deriveWidgetScene(null, residents, today, noon.plusSeconds(h * 3_600), WidgetSize.LARGE)
        }
        assertTrue(laterHours.any { it != base })
    }

    @Test
    fun `探索中は、入力に含まれていても編成メンバーは場面に出ない`() {
        val partyMember = aResident(id = 1L)
        val caretaker = aResident(id = 2L)
        val session = aSession(partyIds = listOf(1L), endAt = noon.plusSeconds(3_600))

        val scene = deriveWidgetScene(
            session,
            listOf(partyMember, caretaker),
            session.snapshot,
            noon,
            WidgetSize.LARGE,
        )

        assertEquals(listOf(2L), scene.residents.map { it.resident.id })
        assertNotEquals(null, scene.bagStage)
    }

    @Test
    fun `L級は2枠換算で上限に入る`() {
        val largeMotif = 4 // ドラゴンの化石（L級）
        val residents = (1L..4L).map { aResident(id = it, motifId = largeMotif) }

        val scene = deriveWidgetScene(null, residents, today, noon, WidgetSize.LARGE)

        assertEquals(3, scene.residents.size) // 2枠 × 3体 = 6枠（上限7）
        assertEquals(1, scene.overflowCount)
    }

    @Test
    fun `Smallの上限は2枠で、超えた分はoverflowCountに出る`() {
        val residents = (1L..3L).map { aResident(id = it) }

        val scene = deriveWidgetScene(null, residents, today, noon, WidgetSize.SMALL)

        assertEquals(2, scene.residents.size)
        assertEquals(1, scene.overflowCount)
    }

    @Test
    fun `ローテーションで30日のうちに全員が表示される`() {
        val residents = (1L..8L).map { aResident(id = it) }
        val shown = mutableSetOf<Long>()

        for (day in 0 until 30) {
            val snapshot = aSnapshot(date = LocalDate.of(2026, 10, 1).plusDays(day.toLong()))
            val scene = deriveWidgetScene(null, residents, snapshot, noon, WidgetSize.MEDIUM)
            shown += scene.residents.map { it.resident.id }
        }

        assertEquals((1L..8L).toSet(), shown)
    }

    @Test
    fun `住人が0体なら留守番役だけで埋める`() {
        val scene = deriveWidgetScene(null, emptyList(), today, noon, WidgetSize.LARGE)

        assertTrue(scene.residents.isEmpty())
        assertEquals(
            listOf(SceneProp.LANTERN, SceneProp.BAG, SceneProp.POT, SceneProp.CART),
            scene.props.map { it.prop },
        )
    }

    @Test
    fun `住人が1体なら留守番役も出るが、3体いれば出ない`() {
        val one = deriveWidgetScene(null, listOf(aResident(id = 1L)), today, noon, WidgetSize.LARGE)
        assertEquals(1, one.residents.size)
        assertTrue(one.props.isNotEmpty())

        val three = deriveWidgetScene(
            null,
            listOf(aResident(id = 1L), aResident(id = 2L), aResident(id = 3L)),
            today,
            noon,
            WidgetSize.LARGE,
        )
        assertTrue(three.props.isEmpty())
    }

    @Test
    fun `空はUNKNOWNなら晴れで描き、時間帯はnowから取る`() {
        val scene = deriveWidgetScene(
            null,
            listOf(aResident(id = 1L)),
            aSnapshot(weather = WeatherKind.UNKNOWN),
            noon,
            WidgetSize.MEDIUM,
        )

        assertEquals(WeatherKind.SUNNY, scene.skyWeather)
        assertEquals(TimeBand.DAY, scene.skyTimeBand) // 12:00 UTC
        assertEquals(WidgetBackground.RUINS_MEDIUM, scene.background)
    }

    @Test
    fun `探索中はスナップショットの天気が空になり、袋の段階が進む`() {
        val session = aSession(
            startedAt = noon.minusSeconds(3_600),
            endAt = noon.plusSeconds(3_600),
            partyIds = listOf(9L),
        )
        val rainy = session.snapshot.copy(weather = WeatherKind.RAIN)

        val scene = deriveWidgetScene(session, listOf(aResident(id = 1L)), rainy, noon, WidgetSize.LARGE)

        assertEquals(WeatherKind.RAIN, scene.skyWeather)
        assertEquals(1, scene.bagStage) // ちょうど中間 → 3段階の真ん中
    }

    @Test
    fun `待機中は袋の段階がない`() {
        val scene = deriveWidgetScene(null, listOf(aResident(id = 1L)), today, noon, WidgetSize.LARGE)

        assertNull(scene.bagStage)
    }

    @Test
    fun `配置されたスポットは重ならず、上限の枠内に収まる`() {
        val residents = (1L..7L).map { aResident(id = it) }

        val scene = deriveWidgetScene(null, residents, today, noon, WidgetSize.LARGE)

        val occupied = scene.residents.flatMap { placed ->
            (placed.spot until placed.spot + placed.slots).toList()
        }
        assertEquals(occupied.size, occupied.toSet().size)
        assertTrue(occupied.all { it in 0 until WidgetSize.LARGE.capacitySlots })
    }
}
