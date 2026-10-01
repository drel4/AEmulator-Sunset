#!/bin/sh
set -eu
TEST_DIR=$(mktemp -d)
TEST_SRC=$(cd "$(dirname "$0")" && pwd)
cc -std=c11 -g -O1 -fsanitize=address,undefined -fno-omit-frame-pointer -pthread \
    -o "$TEST_DIR/camera-smoke" "$TEST_SRC/hal-smoke.c" -ldl
cd "$TEST_DIR"
timeout 20s ./camera-smoke
