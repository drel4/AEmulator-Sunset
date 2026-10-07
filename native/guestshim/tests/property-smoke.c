/* Real ARM transport: host publishes its ACK only after 700ms (> bionic's
 * 250ms shortcut). No ROM, Settings database or keyboard is modified. */
#define AEMU_PROPERTY_SOCKET "property-smoke.sock"
#include "../aemushim.c"
char **environ;
static int test_errno;
int *__errno(void) { return &test_errno; }
AemuFile *fdopen(int fd, const char *mode) { (void)fd; (void)mode; return 0; }
#define CHECK(c) do { if (!(c)) return __LINE__; } while (0)
static int host(int server) {
    for (unsigned i = 0; i < 3; i++) {
        int peer = sys3(285, server, 0, 0);
        CHECK(peer >= 0);
        unsigned char frame[128]; unsigned got = 0;
        while (got < sizeof(frame)) {
            /* Split reads also exercise the full fixed-size protocol frame. */
            long n = sys3(3, peer, (long)(frame + got), 1);
            CHECK(n == 1); got++;
        }
        CHECK(frame[0] == 1 && frame[1] == 0 && frame[2] == 0 && frame[3] == 0);
        CHECK(frame[4] == 's' && frame[31] == 0 && frame[35] == 0);
        CHECK(frame[36] == (i == 1 ? 0 : '1') && frame[37] == 0 && frame[127] == 0);
        unsigned delay[2] = { i == 2 ? 6 : 0, i == 2 ? 0 : 700000000 };
        sys3(162, (long)delay, 0, 0);
        sys3(6, peer, 0, 0);
    }
    return 0;
}
static int test_main(void) {
    CHECK(__system_property_set(0, "x") == -1 && test_errno == 22);
    CHECK(__system_property_set("12345678901234567890123456789012", "x") == -1 && test_errno == 22);
    char oversized[93]; for (unsigned i = 0; i < 92; i++) oversized[i] = 'x'; oversized[92] = 0;
    CHECK(__system_property_set("x", oversized) == -1 && test_errno == 22);
    CHECK(__system_property_set("sys.settings_secure_version", "1") == -1 && test_errno == 2);
    struct { unsigned short family; char path[108]; } addr;
    for (unsigned i = 0; i < sizeof(addr); i++) ((char *)&addr)[i] = 0;
    addr.family = 1;
    for (unsigned i = 0; i < sizeof(AEMU_PROPERTY_SOCKET); i++) addr.path[i] = AEMU_PROPERTY_SOCKET[i];
    int server = sys3(281, 1, 1, 0);
    CHECK(server >= 0);
    CHECK(sys3(282, server, (long)&addr, sizeof(addr)) == 0);
    CHECK(sys3(284, server, 4, 0) == 0);
    int child = sys3(2, 0, 0, 0);
    CHECK(child >= 0);
    if (child == 0) { int result = host(server); sys3(1, result, 0, 0); for (;;) {} }
    unsigned long start, end;
    CHECK(property_now_ms(&start) == 0);
    CHECK(__system_property_set("sys.settings_secure_version", "1") == 0);
    CHECK(property_now_ms(&end) == 0 && end - start >= 600);
    CHECK(__system_property_set("sys.settings_secure_version", 0) == 0);
    CHECK(property_now_ms(&start) == 0);
    CHECK(__system_property_set("sys.settings_secure_version", "1") == -1 && test_errno == 110);
    CHECK(property_now_ms(&end) == 0 && end - start >= 4900 && end - start < 5800);
    int status = 0;
    CHECK(sys4(114, child, (long)&status, 0, 0) == child && status == 0);
    sys3(6, server, 0, 0);
    sys3(10, (long)AEMU_PROPERTY_SOCKET, 0, 0);
    return 0;
}
void _start(void) {
    int result = test_main();
    const char *message = result ? "property smoke FAILED\n" : "property smoke OK\n";
    sys3(4, 1, (long)message, result ? 22 : 18);
    sys3(1, result, 0, 0);
    for (;;) {}
}
