#!/usr/bin/env python3
"""Rng.kt（SplitMix64・FNV-1a・subSeed）の参照実装。RngTest のゴールデン値はこのスクリプトで生成した。
Kotlin 実装と独立に計算できるため、Rng.kt が壊れていないかの照合に使う。

    python3 tools/rng_vectors.py
"""
M = (1 << 64) - 1
GAMMA, MIX1, MIX2 = 0x9E3779B97F4A7C15, 0xBF58476D1CE4E5B9, 0x94D049BB133111EB

def signed(x): return x - (1 << 64) if x >> 63 else x

def mix(z):
    z = ((z ^ (z >> 30)) * MIX1) & M
    z = ((z ^ (z >> 27)) * MIX2) & M
    return z ^ (z >> 31)

def splitmix64(x): return mix((x + GAMMA) & M)

def fnv1a64(label):
    h = 0xCBF29CE484222325
    for b in label.encode('utf-8'):
        h ^= b; h = (h * 0x100000001B3) & M
    return h

def subseed(seed, label): return splitmix64((seed & M) ^ fnv1a64(label))

class Rng:
    def __init__(self, seed): self.s = seed & M
    def next_long(self):
        self.s = (self.s + GAMMA) & M
        return mix(self.s)
    def next_double(self): return (self.next_long() >> 11) * (1.0 / (1 << 53))

if __name__ == '__main__':
    r = Rng(0); print('Rng(0) first 3:', [signed(r.next_long()) for _ in range(3)])
    r = Rng(42); print('Rng(42) first 3:', [signed(r.next_long()) for _ in range(3)])
    print('Rng(42) nextDouble x2 after reset:', [Rng(42).next_double()])
    for lab in ['encounter', 'quell', 'awaken.count', 'awaken.0.star', '精霊']:
        print(f'fnv1a64({lab!r}) =', signed(fnv1a64(lab)), ' subSeed(42) =', signed(subseed(42, lab)))
