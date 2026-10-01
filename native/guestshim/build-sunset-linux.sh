#!/bin/sh
# Run from the checkout root; SDK location may be overridden with ANDROID_HOME.
set -eu
ANDROID_HOME=${ANDROID_HOME:-/home/nyash/0flfx/Android/Sdk}
NDK=${NDK:-$ANDROID_HOME/ndk/22.1.7171670}
export ANDROID_HOME NDK
cd native/guestshim
sh build.sh
"$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf" -d ../../app/src/main/assets/engines/common/libaemushim.so
cd ../..
if [ "$#" -eq 0 ]; then set -- assembleCloneRelease assembleStandardRelease; fi
sh gradlew "$@" --console=plain --max-workers=2
