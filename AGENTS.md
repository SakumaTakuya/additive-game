# AGENTS.md — 『遺跡に住みつく精霊』PoC 開発規約

Android のホーム画面 Widget に住みつく精霊を、ランタン方式の放置探索で育てて集める PoC。
**このリポジトリの主な実装者は AI エージェント**であり、このファイルはその作業規約である。
サブディレクトリにも `AGENTS.md` がある場合は、そのディレクトリでの作業にはそちらも併せて守ること（近い方が優先）。

設計判断はすべて `docs/` に根拠がある。迷ったら該当する設計書を読み、設計書と矛盾するコードを書かない。
設計書を変える必要があると思ったら、コードを書く前に issue へコメントして止まる（設計書の改訂は人間が判断する）。

---

## 1. ドキュメント地図

| 知りたいこと | 読む場所 |
|---|---|
| ゲーム全体（コアループ・報酬・世界観の前提） | `docs/00_コアループ仕様書.md` |
| 状態遷移・フェーズ導出・異常系・三層サーフェスの表示 | `docs/01_探索セッション状態機械.md` |
| 乱数・ロール・勝率式・性格/材質の表・定数 | `docs/02_決定論的ロールエンジン.md` |
| DBスキーマ・トランザクション・天気の取得・指名覚醒・魔力に還す | `docs/03_データ層.md` |
| Widget・通知・アプリ画面の描き方・検証方法 | `docs/04_プレゼンテーション層.md` |
| 画面遷移・戻る動作・ディープリンク・画面ローカル状態・ワイヤー | `docs/05_アプリUI仕様.md` |
| 権限の方針（使ってよい権限・使わない権限） | `docs/10_PoC実施方針.md` |
| 画像の仕様・ファイル名 | `docs/art/ASSETS.md`、`docs/art/アート発注仕様書.md` |
| 決まったことの経緯（ADR） | `docs/decisions.md` |
| 状態機械の形式仕様 | `formal/` |
| 実装タスク（GitHub issue）と番号対応表 | GitHub Issues、`planning/issue_map.json` |

設計書同士の参照は「設計書02 §5.3」のように書かれている。番号はファイル名の先頭2桁に対応する。

---

## 2. 作業の進め方（issue 駆動）

1. **作業単位は GitHub issue 1件**。issue 本文に書かれていない要求は実装しない。「ついで」の修正・リファクタもしない
2. **着手前チェック**：`type:impl` ラベルがあること。本文の「依存」に挙がった issue がすべて closed であること。満たさなければ着手せず、issue にその旨をコメントして終わる
3. **1 issue = 1 ブランチ（`issue/<番号>-<短い説明>`）= 1 PR**。PR 本文に `Closes #<番号>` を必ず書く
4. **受け入れ条件はテストとして先に書く**。issue の受け入れ条件にテスト名が書かれていれば、その名前で作る
5. `./gradlew check` がグリーンで、受け入れ条件をすべて満たしてから PR を作る。満たせないときは**条件を勝手に緩めず**、PR を作らずに issue へ状況をコメントする
6. 変更ファイルが 5 個を超えそうなら、実装前に計画を issue にコメントして人間の返事を待つ
7. 「スコープ外」に書かれたファイルには触れない
8. 別の issue に移るときは、前の作業の文脈を持ち込まない（新しいセッションで始める）

### 決まっていない値に出会ったとき

- 設計書に「叩き台」の値があるなら、その値で実装し、定数の横に `// TODO(DEC-xx): 叩き台` と決定 issue の番号を書く（`planning/issues/DEC.md`）
- 設計書に値も叩き台もないなら、**それらしい値を作らない**。issue にコメントして止まる
- 「要確定」「未確定」と書かれた項目を自分の判断で確定させない

---

## 3. アーキテクチャ原則（違反＝バグ）

| # | 原則 | 実装上の意味 | 出典 |
|---|---|---|---|
| A | 永続化するのは不変の事実のみ | `SessionPhase` を DB・設定に保存しない | 01 §2 |
| B | フェーズは純関数で毎回導出 | 表示は必ず `derivePhase(record, now)` を通す。サーフェスごとの状態を持たない | 01 §2 |
| C | アラーム・Worker は状態を変えない | AlarmManager・WorkManager のコールバックで DB に書かない。再描画（Refresh）だけ | 01 §2 |
| D | ハンドラは画面の表示を信用しない | 回収の前に必ず永続状態から `derivePhase()==RETURNED` を評価し直す。TLA+ が反例を出している（`formal/MC_Buggy.cfg`） | 01 §2・§8 |
| E | 外部の値は点火時に固定する | 天気・日付・時間帯は Ignite で `ContextSnapshot` に固定。回収時に取り直さない | 02 §2 |
| F | エンジンは純関数 | `:core:roll` は時刻・I/O・プラットフォーム API に触れない | 02 §2 |
| G | サーフェスは UiState の純関数レンダラ | 判定は `:core:ui` のマッパーに集約し、JVM テストで検証する。Widget・通知・画面で同じ判定を書き直さない | 04 §2 |

状態を変える操作は **Ignite と Collect の2つだけ**。3つ目が要りそうなら、それは設計変更である（設計書01の改訂と TLA+ の再検査が先）。

---

## 4. 禁止事項

- `kotlin.random.Random`・`java.util.Random`・`Math.random()` を使う（乱数は `dev.seirei.core.roll.Rng` だけ）。例外は**シードの採番**1箇所で、`:app` の `SeedSource` 実装だけが `java.security.SecureRandom` を使ってよい（E3-01）
- `Instant.now()`・`System.currentTimeMillis()`・`LocalDate.now()` を `:app` の `AppContainer` 以外で呼ぶ（現在時刻は `Clock` 経由。ADR D-007）
- `:core:model`・`:core:domain`・`:core:roll` に Android 依存を足す
- `core/roll/.../Rng.kt` のアルゴリズム・定数・関数の意味を変える（全ゴールデンテストと過去セッションの再現性が壊れる）
- ストリームラベル文字列（`"encounter"`、`"quell"`、`"awaken.count"`、`"awaken.$i.star"`、`"named.star"` など）の変更
- `Personality` の並べ替え・削除（並び順が DB に保存される ID。追加は末尾のみ。02 §5.3）
- `LogEvent` の型・フィールドの追加と変更（02 §4 で凍結）
- Live Updates 通知にカスタム RemoteViews を使う（promoted の要件違反。04 §4.1）
- 設計書にない権限を `AndroidManifest.xml` に足す（`docs/10_PoC実施方針.md`）
- ゴールデンテストの期待値を、意図した仕様変更なしに書き換える
- Hilt・Dagger・Koin などの DI フレームワークを入れる（手動 DI。ADR D-006）

---

## 5. モジュールと依存の向き

```
:app → :feature:* / :widget / :notification / :core:*
:feature:*, :widget, :notification → :core:ui, :core:data, :core:domain, :core:model
:core:ui   → :core:roll, :core:model
:core:data → :core:database, :core:domain, :core:roll, :core:model
:core:domain → :core:roll, :core:model
:core:roll → :core:model
:core:database → :core:model
:core:model → （なし）
```

- `:feature:*` 同士、`:widget` と `:notification` の間は直接依存しない（01 §6）
- Kotlin パッケージ：`dev.seirei.core.<module>`、`dev.seirei.widget`、`dev.seirei.notification`、`dev.seirei.feature.<name>`、`dev.seirei.ruins`（:app）
- DI は手動。`SeireiApplication.container`（`AppContainer`）だけがシングルトンの置き場所

---

## 6. コマンド

```bash
./gradlew check                         # 全モジュールのテスト＋lint。PR 前に必ず通す
./gradlew :core:roll:test               # エンジンだけ素早く回す
./gradlew :app:assembleDemoDebug        # フレーバ込みのタスク名で書く（standard / demo）
./gradlew :app:installDemoDebug
adb shell am start -n dev.seirei.ruins.demo/dev.seirei.ruins.MainActivity
adb shell am start -d app://collect     # ディープリンクの確認
adb exec-out screencap -p > /tmp/screen.png

python3 tools/roll_sim.py               # 確率の事前シミュレーション
python3 tools/rng_vectors.py            # Rng の参照値
python3 tools/gen_placeholders.py       # 仮画像（足りないファイルだけ作る）
tools/tlc.sh                            # TLA+ の再検査
python3 tools/check_test_style.py       # テストの書き方の機械検査
```

**フレーバ**：`standard`（本番相当・探索120分）と `demo`（顧客デモ用・探索3分、`dev.seirei.ruins.demo`）。定数セットはフレーバで決まり、実行時に切り替えない（ADR D-003）。デモ定数で「灯す→通知→帰還→開封」が3分で一周する。

---

## 7. 保護されたパス

次のパスを変える PR は CI の `protected-paths` ジョブで落ちる。人間が `design-change` ラベルを付けたときだけ通る。issue に許可がない限り触らない。

- `docs/`、`formal/`、`AGENTS.md`（各階層）、`.github/`
- `gradle/libs.versions.toml`
- `core/roll/src/main/kotlin/dev/seirei/core/roll/Rng.kt`

---

## 8. 変更手順が決まっているもの

**確率・定数を変えるとき**
1. `tools/roll_sim.py` の定数を変更後の値にして回し、★5率・★4+率・3軸期待値が設計書02 §5.2・§7 の階層を保つか確認する
2. `RollConstants` を変える
3. ゴールデンテストの期待値は**意図した変更のときだけ**更新し、PR 本文に変えた定数と設計書の § を書く

**状態機械を変えるとき**
1. `formal/ExplorationSession.tla` を先に変えて `tools/tlc.sh` を通す
2. それから `derivePhase`・ハンドラ・ガードを変える
3. 異常系カタログ（設計書01 §7）に対応するテストを足す

---

## 9. 検証

- **純 Kotlin 層**：`./gradlew test` だけで完結させる。エミュレータを要求しない
- **Room**：Bundled SQLite ドライバでインメモリ DB を作り、JVM テストで回す（ADR D-012）
- **UiState マッパー**：フェーズ×データの表駆動テスト（04 §7）
- **Widget・通知・画面**：エミュレータでスクリーンショットを撮り、PR に添付する
- **E2E**：`scripts/e2e_demo.sh`（E8-08 で作る）で demo フレーバを3分で一周させる

### テストの書き方

- **仕様テスト**は対象機能が呼び出し側に約束する振る舞いを、ドメインの言葉で確認する。対象のテストディレクトリ直下に置き、機能を読む入口となる代表例から、異常系、バリエーションや境界条件へ並べる。複数段の処理は段ごとの仕様テストに分け、入口のテストから流れをたどれるようにする。
- **保証テスト**は決定論、不変条件、統計的性質、全件表照合、実装間の一致など、仕様を横断・網羅する性質を確認する。対象のテストディレクトリに `guarantee/` を設けて置き、仕様テストと役割を混ぜない。
- テスト関数名は日本語で、条件と期待する振る舞いが読める文にする。実装関数名や「works」のような曖昧な表現だけにしない。
- テストは1つの振る舞いまたは性質に焦点を当てる。関連する入力例を表で確認する場合も、失敗時にどの条件が崩れたか分かる単位にまとめる。
- フィクスチャは共有する意味のある既定値や生成手段に限り、名前から内容が分かるものにする。テスト本文に数値IDや日付を直接書かず、`Fixtures.kt` の語彙（例：`member(Material.WATER, 3, Personality.CALM)`、`dayOf(DayOfWeek.MONDAY)`）で状況を表す。個別テストの意図を隠す過度に汎用的なビルダーや、テスト専用の振る舞いを本番コードへ持ち込まない。
- 固定の期待値には、設計書の節、仕様の式、または手計算など、値の導出元を近くのコメントや KDoc に記す。実装と同じ処理を呼び出して期待値を作る自己照合は避ける。
- 上位層のテストでは、その層が保証する契約を確認する。Fake は I/O など外部との境界に限り、下位層の実装を置き換えるために使わない。下位層ですでに保証しているアルゴリズムや全入力表を上位層で再テストせず、必要なら下位層のテストへ委ねる。
- ファイル構成や命名の具体例は、同じモジュールの仕様テストと `guarantee/` を参照する。
- 機械的に確かめられる規則は `tools/check_test_style.py` が CI で検査する：テスト名が日本語の文であること、テストクラスに KDoc があること、`:core:roll` の仕様テストで `PartyMember(` やモチーフIDの数値を直接書かないこと（`Fixtures.kt` を使う）、仕様テストのループが1,000回以下であること。検査対象のモジュールはスクリプト冒頭の `TARGET_DIRS` で、規約に揃えたモジュールから足していく。

---

## 10. レビュー（ADR D-004）

- 純 Kotlin 層（`:core:model`・`:core:domain`・`:core:roll`）：CI グリーン＋人間の承認でマージ
- Android 依存層（`:core:database` 以降）：加えて差分レビューと、画面に関わる変更ならスクリーンショットを必須とする

---

## 11. 現在の状態（2026-10-11）

- 骨組みと凍結部分：`derivePhase`・`ContextSnapshot`・`WeatherKind`/`TimeBand`（`:core:model`）、`Rng`・`subSeed`（`:core:roll`）、`Assets.kt`、仮画像57点
- GitHub に Epic 9件・issue 62件を起票済み。対応表は `planning/issue_map.json`。issue 本文が作業の正で、実装順は `planning/README.md` の依存図に従う
- E0-01（GitHub issue #23）のローカル検証は `./gradlew check` と standard/demo の APK assemble が成功。TLA+ と参照スクリプトも成功。CI が緑になって issue が閉じるまで後続 issue に着手しない
