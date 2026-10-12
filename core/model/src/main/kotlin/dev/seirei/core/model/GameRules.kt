package dev.seirei.core.model

import java.time.DayOfWeek
import java.time.LocalDate

/** 材質ごとの天気相性と曜日祭壇（設計書02 §5.3）。永続化は name を使うこと。 */
enum class Material(
    val goodWeather: WeatherGroup,
    val badWeather: WeatherGroup?,
    val shrineDay: DayOfWeek,
) {
    WATER(WeatherGroup.WET, null, DayOfWeek.WEDNESDAY),
    EARTHENWARE(WeatherGroup.WET, null, DayOfWeek.SATURDAY),
    STONE(WeatherGroup.CLOUD, null, DayOfWeek.SUNDAY),
    BONE(WeatherGroup.CLOUD, null, DayOfWeek.MONDAY),
    METAL(WeatherGroup.SUN, null, DayOfWeek.FRIDAY),
    FIRE(WeatherGroup.SUN, WeatherGroup.WET, DayOfWeek.TUESDAY),
    PLANT(WeatherGroup.SUN, null, DayOfWeek.THURSDAY),
}

enum class SizeClass(val dp: Int) {
    S(48),
    M(72),
    L(120),
}

enum class ColorTier { NORMAL, RARE, GLOW }

data class Motif(
    val id: Int,
    val displayName: String,
    val material: Material,
    val sizeClass: SizeClass,
) {
    companion object {
        /** ルール表はコード定義であり、DBには格納しない（設計書03 §3）。 */
        val ALL: List<Motif> = listOf(
            Motif(1, "スライムの精霊", Material.WATER, SizeClass.M),
            Motif(2, "しずく", Material.WATER, SizeClass.S),
            Motif(3, "壺", Material.EARTHENWARE, SizeClass.M),
            Motif(4, "ドラゴンの化石", Material.BONE, SizeClass.L),
            Motif(5, "結晶", Material.STONE, SizeClass.S),
            Motif(6, "燭台", Material.FIRE, SizeClass.M),
            Motif(7, "門番の石像", Material.STONE, SizeClass.L),
            Motif(8, "壁画の獣", Material.STONE, SizeClass.S),
            Motif(9, "鍵束", Material.METAL, SizeClass.S),
            Motif(10, "供物の果実", Material.PLANT, SizeClass.M),
        )
    }
}

/** 日付の曜日に祀られる材質を返す（設計書02 §5.3）。 */
fun shrineMaterialOf(date: LocalDate): Material = Material.entries.single { it.shrineDay == date.dayOfWeek }
