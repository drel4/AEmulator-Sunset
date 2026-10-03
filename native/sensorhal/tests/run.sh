#!/bin/sh
set -eu
TEST_SRC=$(cd "$(dirname "$0")" && pwd)
TEST_DIR=$(mktemp -d)
cc -std=gnu11 -O1 -g -fsanitize=address,undefined -fno-omit-frame-pointer -pthread \
  -o "$TEST_DIR/sensor-smoke" "$TEST_SRC/smoke.c"
cd "$TEST_DIR"
timeout 20s ./sensor-smoke
