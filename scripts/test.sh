#!/usr/bin/env bash
# Esegue i test JVM. Uso: scripts/test.sh [argomenti gradle extra]
set -e
cd "$(dirname "$0")/.."
source scripts/env.sh
./gradlew testDebugUnitTest -q "$@"
n=$(cat app/build/test-results/testDebugUnitTest/*.xml 2>/dev/null | grep -o '<testcase ' | wc -l)
echo "test JVM: tutti superati ($n test)"
