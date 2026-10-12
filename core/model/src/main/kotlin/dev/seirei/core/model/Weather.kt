package dev.seirei.core.model

import java.time.LocalTime

/** 見た目（空レイヤー）用の天気4種＋中立（設計書02 §2）。永続化は ordinal ではなく name で行うこと。 */
enum class WeatherKind { SUNNY, CLOUDY, RAIN, SNOW, UNKNOWN }

/** ゲーム判定用の天気グループ。雨・雪＝WET、UNKNOWN＝NONE（中立：天気条件の修正値はすべて不発）。 */
enum class WeatherGroup { SUN, CLOUD, WET, NONE }

val WeatherKind.group: WeatherGroup
    get() = when (this) {
        WeatherKind.SUNNY -> WeatherGroup.SUN
        WeatherKind.CLOUDY -> WeatherGroup.CLOUD
        WeatherKind.RAIN, WeatherKind.SNOW -> WeatherGroup.WET
        WeatherKind.UNKNOWN -> WeatherGroup.NONE
    }

/** 時間帯（設計書02 §2）。EVENING はどの性格も発動しない中立帯。 */
enum class TimeBand { DAY, EVENING, NIGHT }

/**
 * 05:00–16:59 昼 ／ 17:00–18:59 夕 ／ 19:00–04:59 夜。
 * エンジンの修正値判定と Widget の空レイヤー（設計書04 §3.2）が同じ関数を使う。
 */
fun timeBandOf(time: LocalTime): TimeBand = when (time.hour) {
    in 5..16 -> TimeBand.DAY
    in 17..18 -> TimeBand.EVENING
    else -> TimeBand.NIGHT
}
