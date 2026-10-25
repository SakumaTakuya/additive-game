package dev.seirei.core.roll

import dev.seirei.core.model.ContextSnapshot
import dev.seirei.core.model.Material
import dev.seirei.core.model.Motif
import dev.seirei.core.model.Party
import dev.seirei.core.model.PartyMember
import dev.seirei.core.model.Personality
import dev.seirei.core.model.TimeBand
import dev.seirei.core.model.WeatherKind
import java.time.DayOfWeek
import java.time.LocalDate

/*
 * :core:roll のテストで使う語彙。
 * テスト本文にモチーフIDや日付の数値を書かず、判定に効く「材質・★・性格・天気・時間帯・曜日」だけで状況を書けるようにする。
 */

/** 住人を材質で作る。修正値の判定に効くのは材質だけなので、モチーフはその材質の先頭を使う。 */
internal fun motifId(material: Material): Int = Motif.ALL.first { it.material == material }.id

internal fun member(material: Material, star: Int, personality: Personality): PartyMember = PartyMember(
    motifId = motifId(material),
    star = star,
    personalityId = personality.id,
)

/** 先頭が隊長。 */
internal fun partyOf(vararg members: PartyMember): Party = Party(members.toList())

internal val emptyParty: Party = Party(emptyList())

/** 2026-01-12（月）〜18（日）の週。月齢は23〜29で満月窓（13以上17未満）にかからないので、月夜好きは発動しない。 */
internal fun dayOf(dayOfWeek: DayOfWeek): LocalDate = LocalDate.of(2026, 1, 12).plusDays(dayOfWeek.value - 1L)

/** 2026-02-03（火）、月齢15.9。満月窓の内側。 */
internal val fullMoonDay: LocalDate = LocalDate.of(2026, 2, 3)

/** 点火時の状況。省略した項目は「天気未取得・昼・月曜」で、どれも修正値を発動させにくい側に倒してある。 */
internal fun snapshot(
    weather: WeatherKind = WeatherKind.UNKNOWN,
    timeBand: TimeBand = TimeBand.DAY,
    day: DayOfWeek = DayOfWeek.MONDAY,
    date: LocalDate = dayOf(day),
): ContextSnapshot = ContextSnapshot(date = date, timeBand = timeBand, weather = weather)

/** 修正値が一つも発動しない状況：天気未取得（天気の相性は不発）・昼・月曜（祭壇は骨）・満月でない。 */
internal val neutralSnapshot: ContextSnapshot = snapshot()

/**
 * [neutralSnapshot] で矢印が0本になる編成。水の住人（月曜の祭壇ではない）で、性格は夜行性（昼は不発）。
 * 勝率は ★ と人数だけで決まる。
 */
internal fun neutralParty(vararg stars: Int): Party =
    Party(stars.map { member(Material.WATER, it, Personality.NOCTURNAL) })

/**
 * 勝率が上限 0.90 に張り付く編成。★5×3、隊長は先導役、残り2体は週末に強いのんびり屋。
 * [strongWeekendSnapshot] と組み合わせると素の合計は 1.05 になる（設計書02 §5.2 の表の最下行）。
 */
internal val strongestParty: Party = partyOf(
    member(Material.WATER, 5, Personality.LEADER),
    member(Material.WATER, 5, Personality.RELAXED),
    member(Material.WATER, 5, Personality.RELAXED),
)

/** 土曜。天気未取得で、水の祭壇（水曜）ではない。 */
internal val strongWeekendSnapshot: ContextSnapshot = snapshot(day = DayOfWeek.SATURDAY)
