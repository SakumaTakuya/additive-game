package dev.seirei.ruins

import android.content.Context
import dev.seirei.core.domain.SeedSource
import dev.seirei.core.model.Clock
import dev.seirei.core.roll.RollConstants
import java.security.SecureRandom
import java.time.Instant

/**
 * 手動DIコンテナ（ADR D-006）。中身は E0-03 以降の各issueで足していく。
 * ここで生成したものだけをアプリ全体で共有する（シングルトンの置き場所はここだけ）。
 */
class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext

    /** 現在時刻の唯一の取得元（ADR D-007）。他の場所で Instant.now() を呼ばない。 */
    val clock: Clock = Clock { Instant.now() }

    /** セッション用シードの唯一の本番採番元（E3-01）。 */
    val seedSource: SeedSource = SecureRandomSeedSource()

    /** フレーバ（standard / demo）。定数セットの選択はフレーバ別ソースセットの [FlavorConfig] が行う。 */
    val flavor: FlavorConfig = FlavorConfig

    /** ビルドフレーバに対応するロール定数セット（設計書02 §6）。 */
    val rollConstants: RollConstants = FlavorConfig.rollConstants
}

private class SecureRandomSeedSource : SeedSource {
    private val secureRandom = SecureRandom()

    override fun nextSeed(): Long = secureRandom.nextLong()
}
