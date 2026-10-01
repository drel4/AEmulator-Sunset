/* Exercise real ARM socket lifecycle; force only the unsupported QEMU branch. */
#define NETMGR_TEST_FORCE_FALLBACK 1
#include "../aemushim.c"
char **environ;
static int test_errno;
int *__errno(void) { return &test_errno; }
AemuFile *fdopen(int fd, const char *mode) { (void)fd; (void)mode; return 0; }
#define CHECK(c) do { if (!(c)) return __LINE__; } while (0)
struct nm_pollfd { int fd; short events, revents; };
static int test_main(void) {
    int fd = socket(16, 3 | 04000 | 02000000, 16);
    CHECK(fd >= 0);
    CHECK(sys3(55, fd, 1, 0) & 1);
    CHECK(sys3(55, fd, 3, 0) & 04000);
    struct nm_addr addr; unsigned len = sizeof(addr);
    CHECK(getsockname(fd, &addr, &len) == 0 && len == 12);
    CHECK(addr.family == 16 && addr.pid == nm_pid() && addr.groups == 0);
    addr.groups = 1;
    CHECK(bind(fd, &addr, sizeof(addr)) == -1 && test_errno == 93);
    addr.groups = 0x40000000;
    CHECK(bind(fd, &addr, sizeof(addr)) == 0);
    CHECK(bind(fd, &addr, sizeof(addr)) == -1 && test_errno == 22);
    CHECK(getsockname(fd, &addr, &len) == 0 && addr.groups == 0x40000000);
    unsigned char shortaddr[2]; len = 2;
    CHECK(getsockname(fd, shortaddr, &len) == 0 && len == 12 && shortaddr[0] == 16);
    int copy = sys3(41, fd, 0, 0);
    CHECK(copy >= 0);
    sys3(6, fd, 0, 0);
    len = sizeof(addr);
    CHECK(getsockname(copy, &addr, &len) == 0 && addr.family == 16);
    int reused = socket(16, 3, 16);
    CHECK(reused == fd); // old endpoint still alive through dup; name must differ
    sys3(6, reused, 0, 0);
    unsigned packet[5] = { 20, 1, 1, 123, 13 };
    struct nm_iov iov = { packet, sizeof(packet) };
    addr.pid = 0;
    struct nm_msg msg = { &addr, sizeof(addr), &iov, 1, 0, 0, 0 };
    CHECK(sendmsg(copy, &msg, 0) == sizeof(packet));
    unsigned parent_pid = nm_pid();
    long child = sys3(2 /* fork */, 0, 0, 0);
    CHECK(child >= 0);
    if (child == 0) {
        struct nm_addr inherited; unsigned inherited_len = sizeof(inherited);
        int ok = getsockname(copy, &inherited, &inherited_len) == 0
            && inherited.pid == parent_pid;
        struct nm_un inherited_name;
        ok = ok && nm_identity(copy, &inherited_name) && inherited.pid != nm_pid()
            && sendmsg(copy, &msg, 0) == sizeof(packet);
        sys3(1, ok ? 0 : 1, 0, 0);
        for (;;) {}
    }
    int status = -1;
    CHECK(sys4(114 /* wait4 */, child, (long)&status, 0, 0) == child && status == 0);
    struct nm_pollfd pollfd = { copy, 1, 0 };
    CHECK(sys3(168, (long)&pollfd, 1, 10) == 0); // no fabricated modem/echo
    CHECK(sys3(297, copy, (long)&msg, 0) == -11); // nonblocking recvmsg EAGAIN
    addr.groups = 1;
    CHECK(sendmsg(copy, &msg, 0) == -1 && test_errno == 93);
    CHECK(sendmsg(copy, 0, 0) == -1 && test_errno == 14);
    sys3(6, copy, 0, 0);
    int ordinary = socket(1, 2, 0);
    CHECK(ordinary >= 0);
    struct nm_un name;
    CHECK(!nm_identity(ordinary, &name)); // reused fd is not stale
    len = sizeof(name);
    CHECK(getsockname(ordinary, &name, &len) == 0 && name.family == 1);
    sys3(6, ordinary, 0, 0);
    int pair[2];
    CHECK(sys4(288 /* socketpair */, 1, 2, 0, (long)pair) == 0);
    msg.name = 0; msg.namelen = 0;
    CHECK(sendmsg(pair[0], &msg, 0) == sizeof(packet));
    unsigned received[5];
    CHECK(sys3(3, pair[1], (long)received, sizeof(received)) == sizeof(received));
    for (unsigned i = 0; i < 5; i++) CHECK(received[i] == packet[i]);
    sys3(6, pair[0], 0, 0); sys3(6, pair[1], 0, 0);
    CHECK(socket(16, 3, 123456) == -1); // no fallback for unrelated protocols
    return 0;
}
void _start(void) {
    int result = test_main();
    const char *message = result ? "netmgr smoke FAILED\n" : "netmgr smoke OK\n";
    sys3(4, 1, (long)message, result ? 20 : 16);
    sys3(1, result, 0, 0);
    for (;;) {}
}
