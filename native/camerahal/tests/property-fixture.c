/* Empty legacy property area: required by stock Bionic before entering main. */
#include <stdio.h>
#include <stdint.h>
#include <string.h>
int main(int argc, char **argv) {
    if (argc != 2) return 1;
    uint32_t area[16384] = {0};
    area[2] = 0x504f5250; area[3] = 0x45434f76;
    // The old Sony loader ignores this property; do not change global ro.hardware.
    area[0] = 1; area[8] = (18u << 24) | 4096u;
    unsigned char *property = (unsigned char *)area + 4096;
    memcpy(property, "ro.hardware.camera", 18);
    *(uint32_t *)(property + 32) = 9u << 24;
    memcpy(property + 36, "aemu_host", 9);
    FILE *out = fopen(argv[1], "wb");
    if (!out) return 2;
    int ok = fwrite(area, 1, sizeof(area), out) == sizeof(area);
    return fclose(out) || !ok;
}
