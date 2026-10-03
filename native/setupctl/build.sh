#!/bin/sh
set -eu
cd "$(dirname "$0")"
SDK=${ANDROID_HOME:?ANDROID_HOME required}
mkdir -p build/classes build/dex
javac --release 8 -cp "$SDK/platforms/android-36/android.jar" -d build/classes src/app/aemu/setup/*.java
"$SDK/build-tools/36.0.0/d8" --min-api 9 --lib "$SDK/platforms/android-36/android.jar" --output build/dex build/classes/app/aemu/setup/*.class
jar cf ../../app/src/main/assets/engines/common/aemu-setup.jar -C build/dex classes.dex
