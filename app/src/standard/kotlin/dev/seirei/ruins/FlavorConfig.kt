package dev.seirei.ruins

import dev.seirei.core.roll.RollConstants

/** standard フレーバ：本番相当の定数（設計書02 §6）。 */
object FlavorConfig {
    const val NAME = "standard"
    const val IS_DEMO = false
    val rollConstants: RollConstants = RollConstants.POC
}
