#!/usr/bin/env bash
# Compila l'APK firmato, esegue i test e lo copia in dist/.
# Uso: scripts/build-apk.sh
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/env.sh

if [ ! -f keystore.properties ]; then
  echo "Manca keystore.properties: senza la chiave di firma non si possono installare aggiornamenti." >&2
  exit 1
fi

./gradlew clean testDebugUnitTest assembleRelease

version=$(grep -m1 'versionName' app/build.gradle.kts | sed -E 's/.*"(.*)".*/\1/')
mkdir -p dist
apk="dist/GymLog-$version.apk"
cp app/build/outputs/apk/release/app-release.apk "$apk"
apksigner verify --verbose "$apk" | grep -E "Verified using v2 scheme"
echo "APK pronto: $apk"
