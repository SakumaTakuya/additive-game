# :core:roll — 作業メモ

純 Kotlin の決定論エンジン（設計書02）。入力は `resolve(seed, snapshot, party, constants)` の4引数だけ。

- `Rng.kt` は凍結。変えたくなったら止まって issue にコメントする
- パイプラインは ①遭遇 → ②鎮め → ③覚醒数 → ④個体ロール → ⑤ログ・集計 の固定順（02 §4）。順序を入れ替えない
- 乱数はサブシステムごとに `Rng(subSeed(seed, "<label>"))` で取る。1本の Rng を工程をまたいで使い回さない
- ストリームラベルの一覧（凍結）：`encounter` / `quell` / `awaken.count` / `awaken.$i.star` / `awaken.$i.personality` / `awaken.$i.color` / `awaken.$i.motif` / `named.star` / `named.personality` / `named.color`
- `arrowsFor()` は UI のプレビューと MinimalBattleResolver が同じものを呼ぶ。UI 用に別の判定を書かない
- 定数はすべて `RollConstants` に置く。性格・材質の対応表は定数ではなくルールなので `:core:model` の enum に置く
- テスト：ゴールデン・決定論・不変条件・統計・ストリーム独立・修正値表・プレビュー一致・月齢（02 §8）。仕様テストと `guarantee/` の分け方、日本語のテスト名、期待値の導出コメントなどはルート `AGENTS.md` §9「テストの書き方」に従う
- ゴールデンテスト（`guarantee/GoldenTest`）の期待値は `src/test/resources/golden/` に置く。意図した変更で再生成するときだけ `GOLDEN_UPDATE=1 ./gradlew :core:roll:test --tests "dev.seirei.core.roll.guarantee.GoldenTest"` を実行し、差分を全行レビューしてからコミットする。通常の `check` では再生成しない
