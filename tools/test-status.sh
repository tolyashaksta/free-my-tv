#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p out/status-tests
"${JAVA_HOME:+$JAVA_HOME/bin/}javac" -encoding UTF-8 -d out/status-tests \
  app/src/io/freemytv/StatusNetworks.java tests/StatusNetworksTest.java
"${JAVA_HOME:+$JAVA_HOME/bin/}java" -cp out/status-tests io.freemytv.StatusNetworksTest
python3 tests/StatusPrefsTest.py
