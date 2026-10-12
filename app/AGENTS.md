# :app — 作業メモ

- 単一 Activity（`MainActivity`）＋ Navigation Compose。ルートは `home` 1つで、他は全部 home の上に積む（05 §2）
- ディープリンク `app://collect` と `app://home` の着地手順は 05 §4 のとおり。着地時にまずバックスタックを home だけにする
- 依存の組み立ては `AppContainer` だけで行う。現在時刻の取得もここの `clock` だけ
- フレーバ別の値は `src/standard/` と `src/demo/` の `FlavorConfig` に置く。`if (BuildConfig.FLAVOR == ...)` の分岐を書かない
- 権限は Manifest のコメントに書いた4つだけ。帰還アラームの権限は E8-01 で決める
