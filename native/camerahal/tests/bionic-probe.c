/* Load the actual release HAL with a ROM's unmodified ARM linker/libc. */
#include <stdio.h>
#include <dlfcn.h>
#include <stdint.h>
int main(int argc, char **argv) {
    if (argc != 2) return 1;
    void *library = dlopen(argv[1], RTLD_NOW);
    if (!library) { puts(dlerror()); return 2; }
    unsigned char *module = dlsym(library, "HMI");
    if (!module || *(uint32_t *)module != 0x48574d54 || *(uint16_t *)(module + 4) != 0x100) return 3;
    int (*count)(void) = *(void **)(module + 0x80);
    if (!count || count() != 0) return 4;
    puts("ROM Bionic camera HAL dlopen/ABI OK");
    return 0;
}
