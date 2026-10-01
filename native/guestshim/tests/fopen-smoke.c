/* ARM Linux smoke test of the actual wrapper. fdopen is a controlled stub:
 * this verifies descriptor/mode handling, not guest bionic's FILE internals. */
#include "../aemushim.c"

char **environ;
static int test_errno, stream_fd, reject_stream;
int *__errno(void) { return &test_errno; }
AemuFile *fdopen(int fd, const char *mode) {
    (void)mode;
    stream_fd = fd;
    if (reject_stream) { test_errno = 12; return 0; }
    return &stream_fd;
}

static int equal(const char *a, const char *b) {
    while (*a && *a == *b) { a++; b++; }
    return *a == *b;
}
#define CHECK(condition) do { if (!(condition)) return __LINE__; } while (0)

static int test_main(void) {
    const char *path = "fopen-smoke.tmp";
    char buf[128];
    CHECK(equal(qt_redirect("/proc/net/xt_qtaguid/stats", buf, sizeof(buf)), "/data/.aemu_qtaguid/stats"));
    CHECK(equal(qt_redirect("/proc/net/xt_qtaguid/ctrl", buf, sizeof(buf)), "/dev/null"));
    CHECK(equal(qt_redirect("/proc/net/xt_qtaguid", buf, sizeof(buf)), "/data/.aemu_qtaguid"));
    CHECK(equal(qt_redirect(path, buf, sizeof(buf)), path));
    CHECK(!fopen(path, "z") && test_errno == 22);
    CHECK(!fopen(0, "r") && test_errno == 22);
    CHECK(!fopen(path, 0) && test_errno == 22);

    CHECK(fopen(path, "w+e"));
    CHECK(sys3(55 /* fcntl */, stream_fd, 1 /* F_GETFD */, 0) & 1);
    CHECK(sys3(55, stream_fd, 3 /* F_GETFL */, 0) % 4 == 2);
    CHECK(sys3(SYS_write, stream_fd, (long)"ab", 2) == 2);
    CHECK(sys3(19 /* lseek */, stream_fd, 0, 0) == 0);
    CHECK(sys3(SYS_read, stream_fd, (long)buf, 2) == 2 && buf[0] == 'a' && buf[1] == 'b');
    sys3(SYS_close, stream_fd, 0, 0);
    CHECK(!fopen(path, "wx") && test_errno == 17);

    CHECK(fopen(path, "ab"));
    CHECK(sys3(19, stream_fd, 0, 1 /* SEEK_CUR */) == 2);
    CHECK(sys3(19, stream_fd, 0, 0) == 0);
    CHECK(sys3(SYS_write, stream_fd, (long)"c", 1) == 1);
    sys3(SYS_close, stream_fd, 0, 0);
    CHECK(fopen64(path, "rb"));
    CHECK(sys3(SYS_read, stream_fd, (long)buf, 4) == 3);
    CHECK(buf[0] == 'a' && buf[1] == 'b' && buf[2] == 'c');
    sys3(SYS_close, stream_fd, 0, 0);

    CHECK(fopen(path, "r+"));
    CHECK(sys3(SYS_write, stream_fd, (long)"d", 1) == 1);
    sys3(SYS_close, stream_fd, 0, 0);
    reject_stream = 1;
    CHECK(!fopen(path, "r") && test_errno == 12);
    CHECK(sys3(SYS_read, stream_fd, (long)buf, 1) == -9);
    reject_stream = 0;
    CHECK(fopen(path, "w"));
    sys3(SYS_close, stream_fd, 0, 0);
    CHECK(fopen(path, "r"));
    CHECK(sys3(SYS_read, stream_fd, (long)buf, 1) == 0);
    sys3(SYS_close, stream_fd, 0, 0);
    CHECK(sys3(10 /* unlink */, (long)path, 0, 0) == 0);
    CHECK(!fopen(path, "r") && test_errno == 2);
    return 0;
}

__attribute__((used)) static void test_exit(void) {
    int result = test_main();
    if (!result) sys3(SYS_write, 1, (long)"ARM fopen smoke: PASS\n", 22);
    sys3(1 /* exit */, result, 0, 0);
    for (;;) {}
}
__attribute__((naked)) void _start(void) {
    __asm__ volatile("bl test_exit");
}
