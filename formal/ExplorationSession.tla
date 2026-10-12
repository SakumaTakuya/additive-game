------------------------- MODULE ExplorationSession -------------------------
(***************************************************************************)
(* 『遺跡に住みつく精霊』 探索セッション状態機械                              *)
(*                                                                         *)
(* 検証したい設計原則:                                                      *)
(*   A. 永続化するのは不変の事実のみ (id, endAt, done)。フェーズは保存しない  *)
(*   B. フェーズは純関数 Phase(session, now) で毎回導出する (pull型)        *)
(*   C. アラーム/タイマーは状態を変更しない。表示の再導出トリガーに過ぎない    *)
(*   D. イベントハンドラは描画済みUI(ステイル可)を信用せず、                  *)
(*      永続状態からの再導出でガードする                                     *)
(*                                                                         *)
(* 環境の敵対的挙動としてモデル化するもの:                                    *)
(*   - アラームの遅延・欠落・重複 (Refreshが任意のタイミングで起きる/起きない) *)
(*   - プロセスキル/再起動 (Kill: 揮発状態が失われる)                        *)
(*   - 壁時計の巻き戻し (ClockBack: ユーザーによる時刻変更)                  *)
(*   - ユーザーのタップ連打 (Collectの任意回数の試行)                        *)
(***************************************************************************)
EXTENDS Naturals

CONSTANTS
  MaxTime,       \* モデル境界: 時刻の上限
  Duration,      \* ランタン燃焼時間 (探索の長さ)
  MaxSessions,   \* モデル境界: 通しで検証するセッション数
  UseUiGuard     \* TRUE = バグ版(UI表示を信用して回収) / FALSE = 設計通り

ASSUME /\ Duration \in Nat \ {0}
       /\ MaxTime \in Nat /\ Duration < MaxTime
       /\ MaxSessions \in Nat \ {0}
       /\ UseUiGuard \in BOOLEAN

VARIABLES
  now,        \* 壁時計 (System.currentTimeMillis 相当)
  session,    \* 永続レコード (Room の session 単一行。設計書01 v0.3〜)。単一の真実
  nextId,     \* セッションID採番
  ui,         \* いずれかのサーフェスに最後に描画されたフェーズ (ステイル可)
  collected,  \* 回収済みセッションIDの集合 (exactly-once 検証用の履歴変数)
  early       \* 「endAt より前に回収が成立した」事故の履歴フラグ

vars == <<now, session, nextId, ui, collected, early>>

NoSession == [id |-> 0, endAt |-> 0, done |-> FALSE]

(* 原則B: フェーズは永続状態と現在時刻から導出される純関数 *)
Phase ==
  IF session.id = 0 \/ session.done THEN "IDLE"
  ELSE IF now < session.endAt       THEN "EXPLORING"
  ELSE                                   "RETURNED"

TypeOK ==
  /\ now \in 0..MaxTime
  /\ nextId \in 1..(MaxSessions + 1)
  /\ ui \in {"IDLE", "EXPLORING", "RETURNED", "STALE"}
  /\ session.id \in 0..MaxSessions
  /\ session.endAt \in Nat
  /\ session.done \in BOOLEAN
  /\ collected \subseteq 1..MaxSessions
  /\ early \in BOOLEAN

Init ==
  /\ now = 0
  /\ session = NoSession
  /\ nextId = 1
  /\ ui = "IDLE"
  /\ collected = {}
  /\ early = FALSE

(* 時間経過。UIは自動では追従しない = 暗黙にステイル化していく *)
Tick ==
  /\ now < MaxTime
  /\ now' = now + 1
  /\ UNCHANGED <<session, nextId, ui, collected, early>>

(* ユーザーによる時刻の巻き戻し (壁時計採用時の敵対ケース) *)
ClockBack ==
  /\ now > 0
  /\ now' = now - 1
  /\ UNCHANGED <<session, nextId, ui, collected, early>>

(* 点火: 編成して灯す。endAt が開始時点で確定する (ランタン方式) *)
Ignite ==
  /\ Phase = "IDLE"
  /\ nextId <= MaxSessions
  /\ now + Duration <= MaxTime
  /\ session' = [id |-> nextId, endAt |-> now + Duration, done |-> FALSE]
  /\ nextId' = nextId + 1
  /\ ui' = "EXPLORING"
  /\ UNCHANGED <<now, collected, early>>

(* 原則C: アラーム発火・アプリ起動・Widget更新はすべてこれ。
   状態を変更せず、永続状態から表示を再導出するだけ。
   遅延 = このアクションがなかなか選ばれない
   欠落 = 一度も選ばれない / 重複 = 連続で選ばれる  … いずれも無害 *)
Refresh ==
  /\ ui' = Phase
  /\ UNCHANGED <<now, session, nextId, collected, early>>

(* プロセスキル/再起動: 揮発状態(描画)が失われる。永続レコードは生き残る *)
Kill ==
  /\ ui' = "STALE"
  /\ UNCHANGED <<now, session, nextId, collected, early>>

(* 回収ハンドラのガード:
   設計通り(原則D) = 永続状態から Phase を再導出して判定
   バグ版          = 最後に描画された ui を信用して判定 *)
CollectGuard ==
  IF UseUiGuard THEN ui = "RETURNED" ELSE Phase = "RETURNED"

(* 回収: タップは任意のタイミングで起こり得る(連打含む)。
   ガードを通過した場合のみ done を立てる *)
Collect ==
  /\ session.id # 0
  /\ ~session.done
  /\ CollectGuard
  /\ session' = [session EXCEPT !.done = TRUE]
  /\ collected' = collected \cup {session.id}
  /\ early' = (early \/ (now < session.endAt))
  /\ ui' = "IDLE"
  /\ UNCHANGED <<now, nextId>>

Next == Tick \/ ClockBack \/ Ignite \/ Refresh \/ Kill \/ Collect

Spec == Init /\ [][Next]_vars

----------------------------------------------------------------------------
(* 検証する不変条件 *)

(* I1: 早期回収なし — endAt 以前に回収が成立することは決してない *)
NoEarlyCollect == early = FALSE

(* I2: 回収は高々1回 — done立てとID集合が一致し続ける(二重回収なし)。
       Collect のガードに ~session.done が含まれることの帰結を確認する *)
CollectConsistent ==
  /\ (session.id # 0 /\ session.done) => session.id \in collected
  /\ (session.id # 0 /\ ~session.done) => session.id \notin collected

(* I3: 表示がステイルでも「実フェーズより先の状態」を見せることはない。
       時計巻き戻しがない場合のみ成立する性質(参考)。
       巻き戻しを許すモデルでは成立しないため既定では未チェック *)
UiNeverAhead ==
  ui = "RETURNED" => (session.id # 0 /\ (session.done \/ now >= session.endAt))
=============================================================================
