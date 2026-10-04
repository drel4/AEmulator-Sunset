/* AEmulator Sunset packaging helper, 2026-10-04. GPL-3.0; see LICENSE.
 * Uses the pinned CM11 filesystem rules, rather than host inode ownership. */
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include "private/android_filesystem_config.h"
int main(void) {
    char path[4096];
    while (fgets(path, sizeof(path), stdin)) {
        size_t n = strlen(path);
        if (!n || path[n - 1] != '\n') return 1;
        path[--n] = 0;
        int directory = n && path[n - 1] == '/';
        if (directory) path[--n] = 0;
        unsigned uid = 0, gid = 0, mode = 0;
        uint64_t capabilities = 0;
        fs_config(path, directory, &uid, &gid, &mode, &capabilities);
        printf("%s %u %u %o\n", path, uid, gid, mode);
    }
    return ferror(stdin) ? 1 : 0;
}
