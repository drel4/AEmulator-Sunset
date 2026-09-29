#!/bin/sh
# Сборка aemu-stubs.jar — гостевых служб-заглушек на Java (dex для Dalvik 4.2+).
# Нужны: JDK (javac), Android SDK (android.jar и d8 из build-tools).
set -e
SDK=${ANDROID_HOME:-$LOCALAPPDATA/Android/Sdk}
JAR="$SDK/platforms/android-36/android.jar"
D8=$(ls -d "$SDK"/build-tools/*/ | sort -V | tail -1)d8
[ -x "$D8" ] || D8="$D8.bat"
OUT=${1:-../../app/src/main/assets/engines/kk/aemu-stubs.jar}
rm -rf build && mkdir -p build/classes
mkdir -p build/hidden
javac --release 8 -cp "$JAR" -d build/hidden $(find hidden -name '*.java')
javac --release 8 -cp "build/hidden;$JAR" -d build/classes $(find src -name '*.java')
"$D8" --min-api 17 --lib "$JAR" --output build $(find build/classes -name '*.class')
python -c "import zipfile;z=zipfile.ZipFile('build/stubs.jar','w',zipfile.ZIP_DEFLATED);z.write('build/classes.dex','classes.dex');z.close()"
cp build/stubs.jar "$OUT"
echo "готово: $OUT"
