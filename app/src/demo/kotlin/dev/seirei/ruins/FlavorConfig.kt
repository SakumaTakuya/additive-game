package dev.seirei.ruins

import dev.seirei.core.roll.RollConstants

/** demo フレーバ：顧客デモ用の定数（探索3分）（設計書02 §6）。 */
object FlavorConfig {
    const val NAME = "demo"
    const val IS_DEMO = true
    val rollConstants: RollConstants = RollConstants.DEMO
}
