package dev.seirei.ruins

import android.content.Context
import dev.seirei.core.model.Clock
import java.time.Instant

/**
 * 手動DIコンテナ（ADR D-006）。中身は E0-03 以降の各issueで足していく。
 * ここで生成したものだけをアプリ全体で共有する（シングルトンの置き場所はここだけ）。
 */
class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext

    /** 現在時刻の唯一の取得元（ADR D-007）。他の場所で Instant.now() を呼ばない。 */
    val clock: Clock = Clock { Instant.now() }

    /** フレーバ（standard / demo）。定数セットの選択はフレーバ別ソースセットの [FlavorConfig] が行う。 */
    val flavor: FlavorConfig = FlavorConfig
}
