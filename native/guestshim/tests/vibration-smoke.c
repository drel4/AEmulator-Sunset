/* Real ARM syscalls, production shim, fake host: never touches a physical motor. */
#define VIBRATION_SOCKET "vibration-smoke.sock"
#include "../aemushim.c"
char **environ;
static int test_errno;
int *__errno(void) { return &test_errno; }
AemuFile *fdopen(int fd, const char *mode) { (void)fd; (void)mode; return 0; }
#define CHECK(c) do { if (!(c)) return __LINE__; } while (0)
static int host(int server) {
    for (unsigned i = 0; i < 6; i++) {
        int peer = sys3(285, server, 0, 0);
        CHECK(peer >= 0);
        unsigned char frame[12]; unsigned got = 0;
        while (got < 12) {
            long n = sys3(3, peer, (long)(frame + got), 12 - got);
            CHECK(n > 0); got += n;
        }
        CHECK(frame[0] == 'V' && frame[1] == 'I' && frame[2] == 'B' && frame[3] == '1');
        unsigned op = frame[4] | ((unsigned)frame[5] << 8) | ((unsigned)frame[6] << 16) | ((unsigned)frame[7] << 24);
        unsigned duration = frame[8] | ((unsigned)frame[9] << 8) | ((unsigned)frame[10] << 16) | ((unsigned)frame[11] << 24);
        CHECK(op == (i == 0 ? 0 : i == 2 ? 2 : 1));
        CHECK(duration == (i == 1 ? 123 : i == 3 ? 0xffffffffu : i >= 4 ? 100 : 0));
        if (i < 4) {
            int reply = i == 0 ? 1 : i == 3 ? -22 : 0;
            /* Deliberately split reply: test accumulation. */
            CHECK(sys3(4, peer, (long)&reply, 1) == 1);
            CHECK(sys3(4, peer, (long)((char *)&reply + 1), 3) == 3);
        } else if (i == 4) {
            unsigned delay[2] = { 0, 700000000 };
            sys3(162, (long)delay, 0, 0); // client must time out
        } // i == 5: close without a reply
        sys3(6, peer, 0, 0);
    }
    return 0;
}
static int test_main(void) {
    CHECK(vibrator_exists() == 0);
    CHECK(vibrator_on(100) == -1 && test_errno == 2);
    struct vb_addr addr;
    for (unsigned i = 0; i < sizeof(addr); i++) ((unsigned char *)&addr)[i] = 0;
    addr.family = 1;
    for (unsigned i = 0; i < sizeof(VIBRATION_SOCKET); i++) addr.path[i] = VIBRATION_SOCKET[i];
    int server = sys3(281, 1, 1, 0);
    CHECK(server >= 0);
    CHECK(sys3(282, server, (long)&addr, sizeof(addr)) == 0);
    CHECK(sys3(284, server, 4, 0) == 0);
    int child = sys3(2 /* fork */, 0, 0, 0);
    CHECK(child >= 0);
    if (child == 0) { int result = host(server); sys3(1, result, 0, 0); for (;;) {} }
    CHECK(vibrator_exists() == 1);
    CHECK(vibrator_on(123) == 0);
    CHECK(vibrator_off() == 0);
    CHECK(vibrator_on(0xffffffffu) == -1 && test_errno == 22);
    CHECK(vibrator_on(100) == -1 && test_errno == 110);
    CHECK(vibrator_on(100) == -1 && test_errno == 32);
    int status = 0;
    CHECK(sys4(114 /* wait4 */, child, (long)&status, 0, 0) == child);
    CHECK(status == 0);
    sys3(6, server, 0, 0);
    sys3(10, (long)VIBRATION_SOCKET, 0, 0);
    return 0;
}
void _start(void) {
    int result = test_main();
    const char *message = result ? "vibration smoke FAILED\n" : "vibration smoke OK\n";
    sys3(4, 1, (long)message, result ? 23 : 19);
    sys3(1, result, 0, 0);
    for (;;) {}
}
