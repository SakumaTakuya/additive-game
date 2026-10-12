#!/usr/bin/env bash
# TLA+ の再検査。MC_Safe は「違反なし」、MC_Buggy は「NoEarlyCollect 違反」になることを確認する。
set -euo pipefail
cd "$(dirname "$0")/../formal"
JAR="${TLA2TOOLS_JAR:-$HOME/.cache/tla2tools-1.8.0.jar}"
if [ ! -f "$JAR" ]; then
  mkdir -p "$(dirname "$JAR")"
  curl -sfL -o "$JAR" https://github.com/tlaplus/tlaplus/releases/download/v1.8.0/tla2tools.jar
fi
run() { java -XX:+UseParallelGC -cp "$JAR" tlc2.TLC -deadlock -metadir "$(mktemp -d)" -config "$1" ExplorationSession.tla 2>&1; }

safe=$(run MC_Safe.cfg)
echo "$safe" | grep -q "No error has been found" || { echo "$safe"; echo "NG: MC_Safe で違反が出た"; exit 1; }
echo "OK: MC_Safe $(echo "$safe" | grep -o '[0-9]* distinct states found')"

buggy=$(run MC_Buggy.cfg || true)
echo "$buggy" | grep -q "Invariant NoEarlyCollect is violated" || { echo "$buggy"; echo "NG: MC_Buggy で反例が出ない"; exit 1; }
echo "OK: MC_Buggy は NoEarlyCollect 違反（原則Dの反例を再現）"
rm -f ExplorationSession_TTrace_*
