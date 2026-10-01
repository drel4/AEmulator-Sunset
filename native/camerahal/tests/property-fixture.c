/* Empty legacy property area: required by stock Bionic before entering main. */
#include <stdio.h>
#include <stdint.h>
int main(int argc, char **argv) {
    if (argc != 2) return 1;
    uint32_t area[16384] = {0};
    area[2] = 0x504f5250; area[3] = 0x45434f76;
    FILE *out = fopen(argv[1], "wb");
    if (!out) return 2;
    int ok = fwrite(area, 1, sizeof(area), out) == sizeof(area);
    return fclose(out) || !ok;
}
