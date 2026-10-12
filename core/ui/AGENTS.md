# :core:ui — 作業メモ

- 画像の参照は `Assets.kt` だけを通す。`R.drawable.xxx` を他のモジュールで直接書かない
- 画像ファイル名は `docs/art/ASSETS.md` が正。名前を変えない。足りない画像は `python3 tools/gen_placeholders.py` で仮画像を作る
- UiState マッパー（`mapWidgetState`・`mapNotificationState`・`deriveWidgetScene`）は Android API を使わない純関数として書き、JVM テストで表駆動検証する（04 §2・§7）
- 独自デザインの部品は3つまで（05 §7）。それ以外は Material 3 の既定部品を使う
