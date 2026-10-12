package dev.seirei.core.model

import java.time.LocalDate

/**
 * 指定日の月齢を日数で返す（設計書02 §5.3）。
 * 判定時刻はその日の12:00 UTCとみなし、天文精度ではなく日単位の近似値を使う。
 */
fun moonAge(date: LocalDate): Double {
    val julianDay = date.toEpochDay() + 2_440_588.0
    val synodicMonth = 29.530588853
    val age = (julianDay - 2_451_550.26) % synodicMonth
    return (age + synodicMonth) % synodicMonth
}

/** 月夜好きの発動範囲（設計書02 §5.3）。 */
fun isFullMoonWindow(date: LocalDate): Boolean = moonAge(date) in 13.0..<17.0
