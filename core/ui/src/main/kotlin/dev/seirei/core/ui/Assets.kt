package dev.seirei.core.ui

import androidx.annotation.DrawableRes

/**
 * 画像リソース参照の唯一の置き場所（docs/art/ASSETS.md）。
 * 本番アセットは drawable-nodpi の同名ファイルを置き換えるだけで反映される。コード変更は不要。
 * モチーフや段階が増えたときだけ、ここの配列に1行足す。
 */
object Assets {
    /** モチーフ番号（1..10）→ 通常・レア色・発光色。添字は motifId - 1。 */
    @DrawableRes val residentNormal = intArrayOf(
        R.drawable.resident_01, R.drawable.resident_02, R.drawable.resident_03, R.drawable.resident_04,
        R.drawable.resident_05, R.drawable.resident_06, R.drawable.resident_07, R.drawable.resident_08,
        R.drawable.resident_09, R.drawable.resident_10,
    )
    @DrawableRes val residentRare = intArrayOf(
        R.drawable.resident_01_rare, R.drawable.resident_02_rare, R.drawable.resident_03_rare,
        R.drawable.resident_04_rare, R.drawable.resident_05_rare, R.drawable.resident_06_rare,
        R.drawable.resident_07_rare, R.drawable.resident_08_rare, R.drawable.resident_09_rare,
        R.drawable.resident_10_rare,
    )
    @DrawableRes val residentGlow = intArrayOf(
        R.drawable.resident_01_glow, R.drawable.resident_02_glow, R.drawable.resident_03_glow,
        R.drawable.resident_04_glow, R.drawable.resident_05_glow, R.drawable.resident_06_glow,
        R.drawable.resident_07_glow, R.drawable.resident_08_glow, R.drawable.resident_09_glow,
        R.drawable.resident_10_glow,
    )

    /** 留守番役（設計書01 §6.2）。 */
    @DrawableRes val propLantern = R.drawable.prop_lantern
    @DrawableRes val propPot = R.drawable.prop_pot
    @DrawableRes val propCart = R.drawable.prop_cart

    /** 発掘袋の膨らみ 0..2（探索中の表示専用演出）。 */
    @DrawableRes val bagStages = intArrayOf(R.drawable.bag_stage_0, R.drawable.bag_stage_1, R.drawable.bag_stage_2)

    /** Widget 背景（Small 2×2 / Medium 4×2 / Large 4×3）。Small の扱いは DEC 待ち（planning/issues/DEC.md）。 */
    @DrawableRes val bgRuinsSmall = R.drawable.bg_ruins_base_small
    @DrawableRes val bgRuinsMedium = R.drawable.bg_ruins_base_medium
    @DrawableRes val bgRuinsLarge = R.drawable.bg_ruins_base_large
    @DrawableRes val bgRareTheme1 = R.drawable.bg_rare_theme_1

    /** 空・光レイヤー。[weather 0..3 = sunny/cloudy/rain/snow][band 0..2 = day/evening/night]。 */
    @DrawableRes val sky = arrayOf(
        intArrayOf(R.drawable.sky_sunny_day, R.drawable.sky_sunny_evening, R.drawable.sky_sunny_night),
        intArrayOf(R.drawable.sky_cloudy_day, R.drawable.sky_cloudy_evening, R.drawable.sky_cloudy_night),
        intArrayOf(R.drawable.sky_rain_day, R.drawable.sky_rain_evening, R.drawable.sky_rain_night),
        intArrayOf(R.drawable.sky_snow_day, R.drawable.sky_snow_evening, R.drawable.sky_snow_night),
    )

    /** 通知（設計書04 §4.1）。smallIcon は白単色＋透過のみ。 */
    @DrawableRes val icLanternSilhouette = R.drawable.ic_lantern_silhouette
    @DrawableRes val icGateStart = R.drawable.ic_gate_start
    @DrawableRes val icHomeEnd = R.drawable.ic_home_end
    @DrawableRes val icMana = R.drawable.ic_mana
}
