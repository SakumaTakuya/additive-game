package dev.seirei.core.ui

import dev.seirei.core.model.ContextSnapshot
import dev.seirei.core.model.ExplorationSession
import dev.seirei.core.model.Motif
import dev.seirei.core.model.Resident
import dev.seirei.core.model.SessionPhase
import dev.seirei.core.model.SizeClass
import dev.seirei.core.model.TimeBand
import dev.seirei.core.model.WeatherKind
import dev.seirei.core.model.derivePhase
import dev.seirei.core.model.timeBandOf
import dev.seirei.core.roll.Rng
import dev.seirei.core.roll.subSeed
import java.time.Instant
import java.time.ZonedDateTime

/** Widget のグリッドサイズと住人の表示容量（設計書04 §3.1。値は叩き台）。 */
// TODO(VER-04): 叩き台。表示上限の実数は実機検証（#55）で確定する
enum class WidgetSize(val capacitySlots: Int) {
    SMALL(2),
    MEDIUM(4),
    LARGE(7),
}

/** Widget の背景。実 drawable への写像は描画側が Assets 経由で行う。 */
enum class WidgetBackground {
    RUINS_SMALL,
    RUINS_MEDIUM,
    RUINS_LARGE,
}

/** 留守番役の小物（設計書01 §6.2）。住人が少ない場面を埋める。 */
enum class SceneProp {
    LANTERN,
    BAG,
    POT,
    CART,
}

data class PlacedResident(
    val resident: Resident,
    val spot: Int,
    val slots: Int,
)

data class PlacedProp(
    val prop: SceneProp,
    val spot: Int,
)

/**
 * Widget に描く場面（設計書04 §3.2）。追加の永続状態ゼロの導出結果。
 * [overflowCount] は上限で入り切らなかった住人の数（「+N」表示用）。
 * [bagStage] は探索中だけ 0..2（袋の膨らみ）、それ以外は null。
 */
data class WidgetScene(
    val background: WidgetBackground,
    val skyWeather: WeatherKind,
    val skyTimeBand: TimeBand,
    val residents: List<PlacedResident>,
    val props: List<PlacedProp>,
    val overflowCount: Int,
    val bagStage: Int?,
)

/**
 * 場面を導出する（設計書04 §3.2、設計書01 §6.2）。
 *
 * - 表示メンバーの選抜は日替わり：[snapshotOrToday] の日付で決定的に並べ替え、
 *   容量（L級は2枠換算）に収まる分だけを選ぶ。同じ日なら全 Widget で同じ顔ぶれになる
 * - 立ち位置の抽選は1時間粒度（DEC-12 暫定）：`Rng(subSeed(epochHour, "widget.scene"))`
 * - 探索中は編成メンバーを入力に含まれていても除外する（留守番組の風景になる）
 * - 帰還時は編成メンバーを優先表示し、残り枠を留守番組で埋める（設計書01 §6.1）
 * - 空の天気は [snapshotOrToday] の値（探索中はセッションのスナップショットを渡すのが呼び出し側の約束）。
 *   `UNKNOWN` は晴れで描く。時間帯は常に [now] のタイムゾーンでの現地時刻から取る
 */
fun deriveWidgetScene(
    session: ExplorationSession?,
    caretakers: List<Resident>,
    snapshotOrToday: ContextSnapshot,
    now: ZonedDateTime,
    sizeClass: WidgetSize,
): WidgetScene {
    val phase = derivePhase(session, now.toInstant())
    val candidates = if (phase == SessionPhase.EXPLORING && session != null) {
        val partyIds = session.partyIds.toSet()
        caretakers.filter { it.id !in partyIds }
    } else {
        caretakers
    }

    val selected = if (phase == SessionPhase.RETURNED && session != null) {
        selectReturned(caretakers, session.partyIds, snapshotOrToday, sizeClass.capacitySlots)
    } else {
        selectForToday(candidates, snapshotOrToday, sizeClass.capacitySlots)
    }
    val placed = placeForThisHour(selected, now.toInstant(), sizeClass.capacitySlots)
    val props = fillWithProps(placed, sizeClass.capacitySlots)

    return WidgetScene(
        background = when (sizeClass) {
            WidgetSize.SMALL -> WidgetBackground.RUINS_SMALL
            WidgetSize.MEDIUM -> WidgetBackground.RUINS_MEDIUM
            WidgetSize.LARGE -> WidgetBackground.RUINS_LARGE
        },
        skyWeather = snapshotOrToday.weather.takeIf { it != WeatherKind.UNKNOWN } ?: WeatherKind.SUNNY,
        skyTimeBand = timeBandOf(now.toLocalTime()),
        residents = placed,
        props = props,
        overflowCount = candidates.size - selected.size,
        bagStage = bagStage(session, phase, now.toInstant()),
    )
}

/**
 * 帰還時の選抜：編成メンバーを優先的に確保し、残り枠を留守番組の日替わりローテーションで埋める
 * （設計書01 §6.1「編成メンバー帰宅」）。
 */
private fun selectReturned(
    residents: List<Resident>,
    partyIds: List<Long>,
    snapshotOrToday: ContextSnapshot,
    capacitySlots: Int,
): List<Resident> {
    val byId = residents.associateBy { it.id }
    val selected = mutableListOf<Resident>()
    var used = 0
    for (id in partyIds) {
        val member = byId[id] ?: continue
        val slots = slotsOf(member)
        if (used + slots <= capacitySlots) {
            selected += member
            used += slots
        }
    }
    val partyIdSet = partyIds.toSet()
    val rest = selectForToday(
        residents.filter { it.id !in partyIdSet },
        snapshotOrToday,
        capacitySlots - used,
    )
    return selected + rest
}

/** 日替わりローテーション：日付で決定的にシャッフルしてから容量に収まる分を選ぶ。 */
private fun selectForToday(
    candidates: List<Resident>,
    snapshotOrToday: ContextSnapshot,
    capacitySlots: Int,
): List<Resident> {
    val rng = Rng(subSeed(snapshotOrToday.date.toEpochDay(), "widget.rotation"))
    val shuffled = candidates.shuffledBy(rng)
    val selected = mutableListOf<Resident>()
    var used = 0
    for (resident in shuffled) {
        val slots = slotsOf(resident)
        if (used + slots <= capacitySlots) {
            selected += resident
            used += slots
        }
    }
    return selected
}

/** 立ち位置：1時間粒度の抽選で並べ替え、空き枠に順に置く。同じ時間枠では変わらない。 */
private fun placeForThisHour(
    selected: List<Resident>,
    now: Instant,
    capacitySlots: Int,
): List<PlacedResident> {
    val epochHour = now.epochSecond / 3_600
    val rng = Rng(subSeed(epochHour, "widget.scene"))
    var nextSpot = 0
    return selected.shuffledBy(rng).map { resident ->
        val slots = slotsOf(resident)
        PlacedResident(resident, spot = nextSpot, slots = slots).also { nextSpot += slots }
    }.sortedBy { it.spot }.takeWhile { it.spot < capacitySlots }
}

private fun fillWithProps(
    placed: List<PlacedResident>,
    capacitySlots: Int,
): List<PlacedProp> {
    if (placed.size > 1) return emptyList()
    val usedSpots = placed.flatMap { (it.spot until it.spot + it.slots) }.toSet()
    val freeSpots = (0 until capacitySlots).filter { it !in usedSpots }
    val props = listOf(SceneProp.LANTERN, SceneProp.BAG, SceneProp.POT, SceneProp.CART)
    return props.zip(freeSpots).map { (prop, spot) -> PlacedProp(prop, spot) }
}

private fun bagStage(session: ExplorationSession?, phase: SessionPhase, now: Instant): Int? {
    if (session == null || phase != SessionPhase.EXPLORING) return null
    val total = session.endAt.epochSecond - session.startedAt.epochSecond
    if (total <= 0) return 2
    val elapsed = now.epochSecond - session.startedAt.epochSecond
    return (elapsed * 3 / total).toInt().coerceIn(0, 2)
}

private fun slotsOf(resident: Resident): Int =
    if (Motif.ALL.single { it.id == resident.motifId }.sizeClass == SizeClass.L) 2 else 1

private fun <T> List<T>.shuffledBy(rng: Rng): List<T> {
    val copy = toMutableList()
    for (i in copy.size - 1 downTo 1) {
        val j = rng.nextInt(i + 1)
        copy[i] = copy[j].also { copy[j] = copy[i] }
    }
    return copy
}
