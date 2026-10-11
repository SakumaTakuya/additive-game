# 形式検証（TLA+）

探索セッション状態機械の仕様と検証設定。詳細は `docs/01_探索セッション状態機械.md` §8。

| ファイル | 内容 | 期待する結果 |
|---|---|---|
| `ExplorationSession.tla` | 仕様本体 | — |
| `MC_Safe.cfg` | 設計どおり（永続状態から再導出してガード） | 648状態を網羅、違反なし |
| `MC_Buggy.cfg` | 原則D違反版（描画済みUIをガードに使う） | `NoEarlyCollect` 違反（7ステップの反例） |

## 再検証

```bash
tools/tlc.sh            # 両方を回し、期待どおりかを判定する（CI でも同じスクリプトを使う）
```

`tla2tools.jar` は https://github.com/tlaplus/tlaplus/releases から取得する（スクリプトが自動で取得する）。

## いつ回すか

`derivePhase`・`Ignite`・`Collect` のガード、状態を変える操作の数（今は2つ）を変えるときは、**コードより先にこの仕様を変えて**再検査する。`formal/` の変更は CI の protected-paths ジョブで `design-change` ラベルを要求される。

2026-10-11：過去チャットから復元し、上表の結果を再現済み（`session` 変数のコメントを Room に合わせた以外は8/26版と同一）。
