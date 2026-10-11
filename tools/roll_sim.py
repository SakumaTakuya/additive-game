#!/usr/bin/env python3
"""確率設計の事前シミュレーション（設計書02 §5.2・§5.4・§7）。

RollConstants の数値を変える前に、変更後の値でこのスクリプトを回し、
「編成を考えると得、考えなくても損はしない」幅（★5率 3%→9% 程度）と
目標の階層（★4+ は数日／2軸は数週間／3軸は天井前提）が保たれることを確認する。

    python3 tools/roll_sim.py              # 設計書02 §5.2 の表を再現
    python3 tools/roll_sim.py --runs 50000 # 試行回数を変える

これは統計の確認用で、Kotlin 実装と乱数列は一致しない（ゴールデン値の生成には使わない）。
定数は設計書02 §5.2・§5.4・§6 の叩き台。Kotlin の RollConstants.POC と揃えておくこと。
"""
import argparse
import math
import random

# --- RollConstants.POC（設計書02 §5.2・§5.4・§6） ---
MEMBER_BASE, STAR_PT, ARROW_PT, P_MAX = 0.12, 0.02, 0.05, 0.90
ENCOUNTER_RATE_PER_HOUR, EXPEDITION_MINUTES = 3.0, 120
QUELL_NORM = 5
STAR_BASE = (0.85, 0.12, 0.03)      # ★1-3 / ★4 / ★5
STAR_BOOSTED = (0.70, 0.20, 0.10)
RARE_COLOR, GLOW_COLOR = 0.08, 0.01
PERSONALITY_COUNT = 12


def win_rate(party):
    """party: [(star, quell_arrows), ...]。空編成は 0（§5.2）。"""
    s = sum(MEMBER_BASE + STAR_PT * (star - 1) + ARROW_PT * arrows for star, arrows in party)
    return min(P_MAX, max(0.0, s))


def poisson(rng, lam):
    l, k, p = math.exp(-lam), 0, 1.0
    while True:
        p *= rng.random()
        if p <= l:
            return k
        k += 1


def star_weights(q):
    t = min(1.0, q / QUELL_NORM)
    return [b * (1 - t) + o * t for b, o in zip(STAR_BASE, STAR_BOOSTED)]


def simulate(party, runs, seed=1):
    rng = random.Random(seed)
    p = win_rate(party)
    lam = ENCOUNTER_RATE_PER_HOUR * EXPEDITION_MINUTES / 60
    q_sum = s5 = s4p = 0
    for _ in range(runs):
        q = sum(1 for _ in range(poisson(rng, lam)) if rng.random() < p)
        q_sum += q
        w = star_weights(q)
        u = rng.random()            # 覚醒1体ぶん（上振れ awakenUpsideTable は未確定のため含めない）
        if u >= w[0]:
            s4p += 1
            if u >= w[0] + w[1]:
                s5 += 1
    star4p = s4p / runs
    three_axis = 1 / (star4p * (1 / PERSONALITY_COUNT) * (RARE_COLOR + GLOW_COLOR))
    return p, q_sum / runs, s5 / runs, star4p, three_axis


ROWS = [
    ("空編成", []),
    ("1体・★3・相性なし", [(3, 0)]),
    ("3体・★2〜3・相性なし", [(2, 0), (3, 0), (3, 0)]),
    ("3体・★2〜3・矢印+3本", [(2, 1), (3, 1), (3, 1)]),
    ("厳選済み・相性の強い日（上限）", [(5, 3), (5, 3), (5, 3)]),
]

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--runs", type=int, default=200_000)
    a = ap.parse_args()
    print(f"{'編成の状態':<28} {'p':>5} {'E[q]':>6} {'★5率':>7} {'★4+率':>7} {'3軸期待':>8}")
    for name, party in ROWS:
        p, eq, s5, s4p, three = simulate(party, a.runs)
        print(f"{name:<28} {p:5.2f} {eq:6.2f} {s5*100:6.1f}% {s4p*100:6.1f}% {three:7.0f}体")
