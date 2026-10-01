/* Real ARM syscall/socket test under qemu-arm, with the production evdev shim. */
#define TRACKBALL_SOCKET "trackball-smoke.sock"
#include "../aemushim.c"
char **environ;
static int test_errno;
int *__errno(void) { return &test_errno; }
AemuFile *fdopen(int fd, const char *mode) { (void)fd; (void)mode; return 0; }
#define CHECK(c) do { if (!(c)) return __LINE__; } while (0)
static int has(unsigned char *b, unsigned bit) { return b[bit / 8] & (1 << (bit % 8)); }
struct test_pollfd { int fd; short events, revents; };
static int test_main(void) {
    struct tb_sockaddr addr;
    addr.family = 1;
    for (unsigned i = 0; i < sizeof(TRACKBALL_SOCKET); i++) addr.path[i] = TRACKBALL_SOCKET[i];
    int server = sys3(281, 1, 1, 0);
    CHECK(server >= 0);
    CHECK(sys3(282, server, (long)&addr, 2 + sizeof(TRACKBALL_SOCKET)) == 0);
    CHECK(sys3(284, server, 4, 0) == 0);
    CHECK(trackball_open("/dev/input/event0", 0) == -4096);
    int fd = open(TRACKBALL_NODE, 04000 | 02000000);
    CHECK(fd >= 0 && trackball_fd(fd));
    CHECK(sys3(55, fd, 1, 0) & 1); // CLOEXEC
    CHECK(sys3(55, fd, 3, 0) & 04000); // NONBLOCK
    int peer = sys3(285, server, 0, 0);
    CHECK(peer >= 0);
    char name[64]; unsigned version = 0;
    CHECK(ioctl(fd, 0x80044501, &version) == 0 && version == 0x10001);
    CHECK(ioctl(fd, 0x80404506, name) > 0 && tb_equal(name, "AEmulator Trackball"));
    unsigned char bits[96];
    CHECK(ioctl(fd, 0x80604521, bits) == 96);
    CHECK(has(bits, 272) && !has(bits, 330) && !has(bits, 103));
    CHECK(ioctl(fd, 0x80084522, bits) == 8 && has(bits, 0) && has(bits, 1));
    CHECK(ioctl(fd, 0x80084520, bits) == 8 && has(bits, 0) && has(bits, 1) && has(bits, 2) && !has(bits, 3));
    CHECK(ioctl(fd, 0x80604523, bits) == 96);
    for (unsigned i = 0; i < 96; i++) CHECK(bits[i] == 0); // no absolute/touch axes
    CHECK(ioctl(fd, 0x80184540, bits) == -1 && test_errno == 25); // unsupported ABS info
    CHECK(ioctl(fd, 0x40044590, 0) == 0);
    CHECK(ioctl(fd, 0x80044501, 0) == -1 && test_errno == 14);
    int copy = sys3(41 /* dup */, fd, 0, 0);
    CHECK(copy >= 0 && trackball_fd(copy));
    sys3(6, fd, 0, 0);
    CHECK(ioctl(copy, 0x80044501, &version) == 0);
    int events[12] = { 1, 2, 2, -6, 1, 2, 0x10002, 3, 1, 2, 0, 0 };
    CHECK(sys3(4, peer, (long)events, sizeof(events)) == sizeof(events));
    struct test_pollfd pollfd = { copy, 1, 0 };
    CHECK(sys3(168, (long)&pollfd, 1, 100) == 1 && (pollfd.revents & 1));
    int got[12];
    CHECK(sys3(3, copy, (long)got, sizeof(got)) == sizeof(got));
    for (unsigned i = 0; i < 12; i++) CHECK(events[i] == got[i]);
    sys3(6, copy, 0, 0); sys3(6, peer, 0, 0); sys3(6, server, 0, 0);
    fd = open("ordinary.tmp", 0102, 0600);
    CHECK(fd >= 0 && !trackball_fd(fd));
    CHECK(ioctl(fd, 0x80044501, &version) == -1 && test_errno == 25);
    sys3(6, fd, 0, 0);
    sys3(10 /* unlink */, (long)"ordinary.tmp", 0, 0);
    sys3(10, (long)TRACKBALL_SOCKET, 0, 0);
    return 0;
}
void _start(void) {
    int result = test_main();
    const char *message = result ? "trackball smoke FAILED\n" : "trackball smoke OK\n";
    sys3(4, 1, (long)message, result ? 23 : 19);
    sys3(1, result, 0, 0);
    for (;;) {}
}
