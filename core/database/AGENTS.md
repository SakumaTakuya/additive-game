# :core:database — 作業メモ

Room（設計書03 §3）。スキーマ version 3 から始める。

- テーブルは `session`・`resident`・`mana`・`collection_receipt`・`app_settings` の5つ。単一行テーブルは `id = 1` 固定（CHECK 制約）
- スキーマ export を有効にし、`core/database/schemas/` をコミットする（差分をレビューで読むため）
- PoC はマイグレーションを書かず `fallbackToDestructiveMigration` でよい（03 §6）
- テストは JVM で Bundled SQLite ドライバのインメモリ DB を使う（ADR D-012）
- `Collect` の4書込（レシート→住人→魔力→回収済み）は1トランザクション。レシートの主キー衝突で全体がロールバックすることをテストで確かめる
