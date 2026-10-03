#!/bin/sh
# AEmulator Sunset CM11 build launcher, added 2026-10-04.
set -eu
base=${CM11_AESS_DIR:-/home/nyash/0drel/cm11-aess}
recipe=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
test -d "$base/src/.repo"
test -f "$base/manifest-resolved.xml"
test -x "$base/tools/jdk7/bin/javac"
python3 "$recipe/prepare.py" "$base"
if docker inspect cm11-aess-build-1 >/dev/null 2>&1; then
    state=$(docker inspect --format '{{.State.Status}}' cm11-aess-build-1)
    printf 'Existing AESS build container: %s. Inspect its logs; do not start a duplicate.\n' "$state"
    exit 1
fi
docker run -d --name cm11-aess-build-1 --network none --cap-drop ALL \
    --security-opt no-new-privileges --memory 5g --memory-swap 8g --cpus 2 \
    --pids-limit 2048 --user 1000:1000 \
    --log-opt max-size=100m --log-opt max-file=2 \
    -v "$base/src:/src" -v "$base/tools:/tools:ro" -v "$recipe:/recipe:ro" \
    --entrypoint /bin/bash \
    sha256:0138c2e800e4631087b9f66bee5d2aa4935335c352182e06cb16408291236e37 \
    /recipe/build-container.sh
