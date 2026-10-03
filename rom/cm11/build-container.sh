#!/bin/bash
set -eo pipefail
# Modern Docker's huge fd limit makes JDK7 allocate an oversized descriptor table.
ulimit -n 4096
export JAVA_HOME=/tools/jdk7
export PATH="$JAVA_HOME/bin:$PATH"
export LC_ALL=C
export BUILD_NUMBER=AESS442-1
export BUILD_USERNAME=aess
export BUILD_HOSTNAME=aess-builder
# Version checks read the first output line; injected JVM options print a banner.
unset _JAVA_OPTIONS JAVA_TOOL_OPTIONS JDK_JAVA_OPTIONS
cd /src
source build/envsetup.sh
lunch cm_aess-userdebug
make -j2 systemimage ramdisk mkbootimg
