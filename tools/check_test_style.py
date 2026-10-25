#!/usr/bin/env python3
"""テストの書き方（AGENTS.md §9）のうち、機械的に確かめられるものを検査する。

対象は TARGET_DIRS 配下の *.kt。違反があれば ファイル:行 と直し方を出して終了コード1で終わる。
判断が要る規則（代表例が先頭か、1テスト1性質か、上位層で下位層を再テストしていないか）はレビューで見る。

使い方: python3 tools/check_test_style.py [リポジトリのルート]
"""
from __future__ import annotations

import os
import re
import sys
from pathlib import Path

# 規約に揃えたモジュールだけを対象にする。揃えたら足す。
TARGET_DIRS = [
    "core/model/src/test",
    "core/roll/src/test",
    "core/domain/src/test",
    "core/testing/src/test",
]

# 仕様テストで本文に直接書かず、フィクスチャ（Fixtures.kt）を使うもの。
FIXTURE_ONLY = {
    "core/roll/src/test": [
        (re.compile(r"\bPartyMember\("), "PartyMember(...) を直接作らず member(Material, ★, Personality) を使う"),
        (re.compile(r"\bmotifId\s*=\s*\d"), "モチーフIDの数値を書かず motifId(Material) を使う"),
    ],
}

SPEC_LOOP_LIMIT = 1_000
JAPANESE = re.compile(r"[\u3040-\u30ff\u3400-\u9fff]")
TEST_FUN = re.compile(r"@Test\s+fun\s+(`[^`]+`|\w+)\s*\(")
TEST_CLASS = re.compile(r"^(?:internal\s+|open\s+|abstract\s+)*class\s+(\w+Test)\b", re.MULTILINE)
KDOC_BEFORE = re.compile(r"\*/\s*(?:@[\w.]+(?:\([^)]*\))?\s*)*$")
REPEAT_OR_ARRAY = re.compile(r"(?:repeat|IntArray)\(\s*([\d_]+)")
RANGE = re.compile(r"(-?[\d_]+)\s*(\.\.|until)\s*(-?[\d_]+)")


def line_of(text: str, index: int) -> int:
    return text.count("\n", 0, index) + 1


def check_file(root: Path, path: Path, target: str) -> list[str]:
    rel = path.relative_to(root).as_posix()
    text = path.read_text(encoding="utf-8")
    is_guarantee = "/guarantee/" in rel
    errors: list[str] = []

    def error(index: int, message: str) -> None:
        errors.append(f"{rel}:{line_of(text, index)}: {message}")

    tests = list(TEST_FUN.finditer(text))
    for match in tests:
        name = match.group(1)
        if not (name.startswith("`") and JAPANESE.search(name)):
            error(match.start(), f"テスト名 {name} は、条件と期待する振る舞いが読める日本語の文にする（例: `先導役が隊長なら、隊長自身を含む全員に+1`）")

    if tests:
        for match in TEST_CLASS.finditer(text):
            if not KDOC_BEFORE.search(text[: match.start()]):
                hint = "何を保証し、どの仕様テストを読めばよいか" if is_guarantee else "対応する設計書の §と、このファイルが答える問い"
                error(match.start(), f"class {match.group(1)} に KDoc を付け、{hint}を書く")

    if not is_guarantee:
        if path.name != "Fixtures.kt":
            for pattern, message in FIXTURE_ONLY.get(target, []):
                for match in pattern.finditer(text):
                    error(match.start(), message)
        for match in REPEAT_OR_ARRAY.finditer(text):
            count = int(match.group(1).replace("_", ""))
            if count > SPEC_LOOP_LIMIT:
                error(match.start(), f"仕様テストのループは {SPEC_LOOP_LIMIT:,} 回まで。統計・網羅の確認は guarantee/ に置く")
        for match in RANGE.finditer(text):
            start = int(match.group(1).replace("_", ""))
            end = int(match.group(3).replace("_", ""))
            count = max(0, end - start + (1 if match.group(2) == ".." else 0))
            if count > SPEC_LOOP_LIMIT:
                error(match.start(), f"仕様テストのループは {SPEC_LOOP_LIMIT:,} 回まで（この範囲は {count:,} 回）。統計・網羅の確認は guarantee/ に置く")

    return errors


def main() -> int:
    root = Path(sys.argv[1] if len(sys.argv) > 1 else ".").resolve()
    errors: list[str] = []
    checked = 0
    for target in TARGET_DIRS:
        for path in sorted((root / target).rglob("*.kt")):
            checked += 1
            errors += check_file(root, path, target)

    for line in errors:
        print(f"::error::{line}" if "GITHUB_ACTIONS" in os.environ else line)
    if errors:
        print(f"\n{len(errors)} 件の違反（{checked} ファイルを検査）。規約は AGENTS.md §9「テストの書き方」。")
        return 1
    print(f"OK（{checked} ファイルを検査）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
