/* Exercise the real streaming loader with maps larger than the old 256 KiB
 * limit, then check stack discovery after the executable map table fills. */
#include "../crash.c"
int sigaction(int sig, const void *act, void *old) {
    (void)sig; (void)act; (void)old; return 0;
}
static int length(const char *s) { int n = 0; while (s[n]) n++; return n; }
static int equal(const char *a, const char *b) {
    while (*a && *a == *b) { a++; b++; } return *a == *b;
}
#define CHECK(condition) do { if (!(condition)) return __LINE__; } while (0)
static int test_main(void) {
    const char *path = "crash-maps.tmp";
    const char *code = "40000000-40001000 r-xp 00002000 00:00 123 /system/lib/lib test.so\n";
    const char *heap = "50000000-50001000 rw-p 00000000 00:00 0 [anon:large map test padding]\n";
    const char *stack = "7fff0000-80000000 rw-p 00000000 00:00 0 [stack:99]";
    long fd = cr_sys3(SYS_open, (long)path, 0x241 /* WRONLY|CREAT|TRUNC */, 0600);
    CHECK(fd >= 0);
    CHECK(cr_sys3(SYS_write, fd, (long)code, length(code)) == length(code));
    for (int i = 0; i < 6000; i++)
        CHECK(cr_sys3(SYS_write, fd, (long)heap, length(heap)) == length(heap));
    CHECK(cr_sys3(SYS_write, fd, (long)stack, length(stack)) == length(stack));
    cr_sys3(SYS_close, fd, 0, 0);
    g_sp = 0x7fff0100; g_stack_end = 0;
    load_maps_from(path);
    CHECK(g_stack_end == 0x80000000 && g_nmaps == 1 && !g_map_limit);
    CHECK(g_ms[0] == 0x40000000 && g_me[0] == 0x40001000 && g_mo[0] == 0x2000);
    CHECK(equal(g_mp[0], "/system/lib/lib test.so"));
    CHECK(find_map(0x40000001) == 0 && find_map(0x40001000) == -1);
    for (int i = 0; i < MAXMAP; i++) parse_map_line("60000000-60001000 r-xp 00000000 00:00 0 /system/lib/extra.so");
    CHECK(g_map_limit && g_nmaps == MAXMAP);
    g_stack_end = 0;
    parse_map_line(stack);
    CHECK(g_stack_end == 0x80000000);
    g_stack_end = 0;
    parse_map_line("7fff0000-80000000 ---p 00000000 00:00 0 [guard]");
    CHECK(g_stack_end == 0);
    parse_map_line("7fff0000-80000000 r");
    CHECK(g_stack_end == 0);
    CHECK(cr_sys3(10 /* unlink */, (long)path, 0, 0) == 0);
    return 0;
}
__attribute__((used)) static void test_exit(void) {
    int result = test_main();
    if (!result) cr_sys3(SYS_write, 1, (long)"ARM crash maps smoke: PASS\n", 27);
    cr_sys3(1 /* exit */, result, 0, 0);
    for (;;) {}
}
__attribute__((naked)) void _start(void) { __asm__ volatile("bl test_exit"); }
