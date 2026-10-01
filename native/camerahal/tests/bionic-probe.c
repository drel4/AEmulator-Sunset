/* Load the actual release HAL with a ROM's unmodified ARM linker/libc. */
#include <stdio.h>
#include <dlfcn.h>
#include <stdint.h>
#include <string.h>
int main(int argc, char **argv) {
    if (argc < 2 || argc > 4) return 1;
    void *library = dlopen(argv[1], RTLD_NOW);
    if (!library) { puts(dlerror()); return 2; }
    unsigned char *module = dlsym(library, "HMI");
    if (!module || *(uint32_t *)module != 0x48574d54 || *(uint16_t *)(module + 4) != 0x100) return 3;
    int (*count)(void) = *(void **)(module + 0x80);
    if (!count || count() != 0) return 4;
    puts("ROM Bionic camera HAL dlopen/ABI OK");
    if (argc >= 3) {
        void *hardware = dlopen(argv[2], RTLD_NOW);
        if (!hardware) { puts(dlerror()); return 5; }
        int (*get_module)(const char *, const void **) = dlsym(hardware, "hw_get_module");
        const void *selected = 0;
        if (!get_module) return 6;
        int result = get_module("camera", &selected);
        if (argc == 4) {
            if (!result || selected) return 7;
            puts("ROM loader ignores class property: missing default reproduced");
        } else {
            if (result || !selected || *(const uint32_t *)selected != 0x48574d54 ||
                strcmp(*(const char *const *)((const char *)selected + 12), "AEmulator host camera")) return 8;
            puts("ROM hw_get_module selected host camera through default fallback");
        }
    }
    return 0;
}
