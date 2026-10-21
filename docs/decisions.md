# 設計決定記録（ADR）

docs-first原則：決定はまず設計書に反映し、ここには「何を・なぜ決めたか」の要約と反映先だけを残す。
設計書と本書が食い違う場合は**設計書が正**。本書は経緯の索引である。

番号は欠番を作らず追記のみ。覆した決定は削除せず「置換」と書く。

---

## D-001: 性格テーブル12種（2026-08-26）— **置換済み**

8/26に12種を決めたが、2026-09の資料v0.4で修正値モデルごと作り直した（設計書02 §5.3）。現行の性格表は設計書02 v0.4以降のもの。8/26版（縁起担ぎ・群れの長・大物狩り・長旅好き等）は使わない。

## D-002: LogEvent は5型・1遭遇最大3イベントで凍結（2026-08-26決定、2026-10-11再反映）

- `Encountered` / `Quelled` / `Failed` / `Found` / `Awakened`。フィールドは設計書02 §4
- 早期帰還はイベント型を足さず `ExpeditionResult.earlyReturn` で表す
- 8/26に決めたが資料v0.4系列に反映されておらず、02 v0.4は「要確定」のままだった。v0.5で再反映
- 反映先：設計書02 v0.5 §4

## D-003: 定数セットはビルドフレーバ standard / demo で固定（2026-08-26決定、2026-10-11再反映）

- 実行時切替は採らない（セッション行に「どの定数で計算したか」を持つ必要が出て、状態機械とスキーマに手が入る）
- `demo` は `applicationIdSuffix ".demo"`＋表示名「精霊 DEMO」。standard と並べてインストールできる
- 名前を `poc` にしなかったのは「どちらが本番相当か」が読めないため
- 反映先：設計書02 v0.5 §6、`app/build.gradle.kts`

## D-004: レビュー方針の段階分け（2026-08-26）

- 純Kotlin層（`:core:model` `:core:domain` `:core:roll`）：CIグリーン＋人間のPR承認でマージ可
- Android依存層（`:core:database` 以降すべて）：加えて差分レビューとスクリーンショット添付を必須
- 反映先：`AGENTS.md`「レビュー」

## D-005: minSdk 34 / compileSdk・targetSdk 36（2026-10-11）

- 設計書04 §4.3のフォールバック階梯は Android 14〜15 までしか定義していない。それより古い端末の表現を誰も決めていないので、対応範囲を階梯に合わせて 14（API 34）以上とする
- 36 は Live Updates（Android 16）の必須ライン。より新しいSDKへの追従は E0-01 で判断する
- 顧客デモ端末に Android 13 以下が含まれるなら、この決定と04 §4.3を一緒に見直す
- 反映先：`gradle/libs.versions.toml`

## D-006: DIは手動（AppContainer）。Hilt は使わない（2026-10-11）

- 依存の受け手に Glance の Receiver・BroadcastReceiver・Worker・Activity が混在し、Hilt だとそれぞれ別の注入口が要る。`Application` が持つ1つの `AppContainer` から全員が取る方が単純
- アノテーション処理を減らし、ビルド失敗の原因を減らす（Agent開発での事故を減らす）
- コンストラクタ注入は維持する（テストではフェイクを直接渡す）
- 反映先：`AGENTS.md`、E0-03

## D-007: 時刻は java.time、現在時刻は Clock インターフェースで注入（2026-10-11）

- `Instant` / `LocalDate` / `LocalTime` / `ZoneId` / `Duration` は `java.time` を使う（minSdk 34 なので desugar 不要）
- 現在時刻は `:core:model` の `fun interface Clock { fun now(): Instant }` を通してだけ取る。`Instant.now()` の直接呼び出しは `:app` の実装1箇所に限る
- テストは `FakeClock` で時計を進める・戻す（設計書01 §7の異常系#7・#8）
- 反映先：`core/model/.../Clock.kt`、`AGENTS.md`

## D-008: resolve() の入力に party を追加（2026-10-11）

- 02 v0.4は `resolve(seed, snapshot, constants)` の3引数としつつ、②鎮め解決が編成（★・性格・材質）を読む。引数が足りていなかった
- 住人の属性は不変で、編成中の個体は還元できないため、回収時に読み直しても決定論は保たれる
- 反映先：設計書02 v0.5 §2

## D-009: 探索時間は RollConstants.expeditionMinutes に置く（2026-10-11）

- 01 §10・02 §4①で「duration」の置き場所が決まっていなかった。Ignite の `endAt` と遭遇生成が別々の値を読むと、デモで「3分の探索に2時間分の遭遇」が起きる
- フレーバで定数が1組に決まる（D-003）ので、同じ値を両方が読めばずれない
- 反映先：設計書02 v0.5 §4・§6

## D-010: エージェント非依存。規約は AGENTS.md に一本化（2026-10-11）

- 実装は Codex・Cursor など Claude Code 以外のエージェントが行う。どちらも `AGENTS.md`（ルートとサブディレクトリ）を読む
- 8/26版にあった `.claude/settings.json` によるパス保護は使えないので、保護は **CI の protected-paths ジョブ＋CODEOWNERS＋ブランチ保護** で行う
- 反映先：`AGENTS.md`、`.github/`

## D-011: パッケージ名（2026-08-26の骨子を継承）

- applicationId：`dev.seirei.ruins`（demo は `dev.seirei.ruins.demo`）
- Kotlinパッケージ：`dev.seirei.core.model` / `.domain` / `.roll` / `.database` / `.data` / `.ui`、`dev.seirei.widget`、`dev.seirei.notification`、`dev.seirei.feature.expedition` / `.collection`、`dev.seirei.ruins`（:app）

## D-012: Room のテストは JVM で回す（2026-10-11）

- `androidx.sqlite` の Bundled SQLite ドライバでインメモリDBを作り、`./gradlew test` で DAO・トランザクションを検証する。エミュレータを要求しない
- 回収の二重実行（設計書03 §4.1）はこのテストで機械的に確認する
- 反映先：E4（データ層）の各issue

## D-013: JSON は kotlinx.serialization（2026-10-11）

- `session.partyJson`、気象庁JSONのパース、Navigation の型安全ルートの3箇所で使う

## D-014: 個体ロールのモチーフ抽選に専用ストリーム awaken.$i.motif を使う（2026-10-11）

- 02 §4 の④は「モチーフ・★・性格・色」を決めるが、§3 のストリーム一覧にモチーフ用がなかった
- 他の軸と同じ形式で `awaken.$i.motif` を足す。モチーフ抽選の重み（DEC-06）を後で変えても★・性格・色が動かない
- 反映先：設計書02 v0.5 §3、`core/roll/AGENTS.md`

## D-015: ★1〜3 の帯内比率は 50:30:20（2026-10-18）

- base・boosted の★1〜3帯を★1・★2・★3へ分ける比率を 50:30:20 とし、鎮め補正では帯内比率を変えない
- 反映先：設計書02 §5.4・§6、`RollConstants.star1To3`

## D-016: 覚醒上振れは最大1体（2026-10-18）

- POC は鎮め数 `q=0〜2` で0%、`q=3` で25%、`q=4` で40%、`q≥5` で50%。DEMO は `q=0` で0%、`q≥1` で50%とする
- 反映先：設計書02 §4・§6、`RollConstants.awakenUpsideTable`

## D-017: 魔力獲得・天井・還元レート（2026-10-18）

- `manaBase=20`、`manaPerQuell=10`、`CEILING_COST=400`。還元は★1=10を基準に★ごとに2倍、レア色×3、発光色×10
- 反映先：設計書02 §6、設計書03 §4.4・§7

## D-018: PoCでは敗北による早期帰還を起こさない（2026-10-18）

- `MinimalBattleResolver` は常に `earlyReturn=false` を返す。`endAt` は不変とし、早期帰還の結果フィールドは将来用に残す
- 反映先：コアループ仕様書 §3、設計書01 §9、設計書02 §4・§5.1
