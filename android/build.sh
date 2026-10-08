#!/usr/bin/env bash
# Builds WalkersClub.apk without Gradle: javac -> d8 -> aapt2 -> zipalign -> apksigner.
set -euo pipefail
cd "$(dirname "$0")"

SDK="${ANDROID_HOME:-$LOCALAPPDATA/Android/Sdk}"
BT="$SDK/build-tools/36.1.0"
JAR="$SDK/platforms/android-35/android.jar"
JBR="C:/Program Files/Android/Android Studio1/jbr/bin"
export JAVA_HOME="C:/Program Files/Android/Android Studio1/jbr"
export PATH="$JBR:$PATH"

rm -rf build && mkdir -p build/classes build/res

python make_icons.py
python prepare_web.py
"$JBR/javac.exe" --release 8 -nowarn -classpath "$JAR" -d build/classes src/com/walkersclub/chengalpattu/*.java 2>&1 | grep -v "obsolete" || true
"$BT/d8.bat" --min-api 24 --lib "$JAR" --output build $(find build/classes -name '*.class')

"$BT/aapt2.exe" compile --dir res -o build/res.zip
"$BT/aapt2.exe" link -I "$JAR" --manifest AndroidManifest.xml -A assets -o build/unsigned.apk build/res.zip
python repack.py

"$BT/zipalign.exe" -f -p 4 build/repacked.apk build/aligned.apk

KS=walkersclub.keystore
if [ ! -f "$KS" ]; then
  "$JBR/keytool.exe" -genkeypair -keystore "$KS" -storepass walkersclub -keypass walkersclub -alias wwc \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Walkers Welfare Club, L=Chengalpattu, C=IN" >/dev/null
fi
"$BT/apksigner.bat" sign --ks "$KS" --ks-pass pass:walkersclub --key-pass pass:walkersclub --out WalkersClub.apk build/aligned.apk
"$BT/apksigner.bat" verify WalkersClub.apk && echo "built $(pwd)/WalkersClub.apk ($(du -k WalkersClub.apk | cut -f1) KB)"
