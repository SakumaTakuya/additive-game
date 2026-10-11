# :core:model — 作業メモ

純 Kotlin。Android 依存・I/O・現在時刻の直接取得を持ち込まない。

- ゲームのルール表（モチーフ10体・材質7種・性格12種・47都道府県）は**コードで定義**し、DB に入れない（設計書03 §3）
- `Personality` と `Material` は設計書02 §5.3 の表を**表駆動テスト**で固定する。表とコードがずれたらコードを直す
- `Personality` の並び順は DB に保存される ID。追加は末尾のみ、並べ替え・削除は禁止
- enum を永続化するときは `name` で保存する方針（ordinal は `Personality` の ID のように設計書が明示したものだけ）
- `derivePhase` は TLA+ の `Phase` と1対1。変えるなら `formal/` が先
- `timeBandOf`・`WeatherKind.group`・`moonAge`・`shrineMaterialOf` は Widget の空とエンジンの判定が共有する。複製しない
