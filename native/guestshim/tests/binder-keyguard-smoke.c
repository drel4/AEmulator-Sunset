/* ARM32 packet ABI and async reentrancy regression, without a ROM patch. */
#include "../aemushim.c"
char **environ;
static int test_errno;
int *__errno(void) { return &test_errno; }
AemuFile *fdopen(int fd, const char *mode) { (void)fd; (void)mode; return 0; }
#define CHECK(c) do { if (!(c)) return __LINE__; } while (0)
static void clear(unsigned *p, unsigned words) { for (unsigned i = 0; i < words; i++) p[i] = 0; }
static void parcel(unsigned *p, const char *name, unsigned len) {
    clear(p, 40); p[1] = len;
    unsigned char *bytes = (unsigned char *)p;
    for (unsigned i = 0; i < len; i++) bytes[8 + 2 * i] = name[i];
}
static void transaction(unsigned *p, unsigned *data, unsigned size, unsigned code, unsigned flags) {
    clear(p, 11); p[0] = BR_TRANSACTION_; p[3] = code; p[4] = flags;
    p[5] = 123; p[6] = 1000; p[7] = size; p[9] = (unsigned)(ulong)data;
}
static int test_main(void) {
    unsigned data[40], media[40], rb[300];
    parcel(data, "com.android.internal.policy.IKeyguardService", 44);
    transaction(rb, data, 100, 8, TF_ONE_WAY_);
    CHECK(defer_async((unsigned char *)(rb + 1)));
    struct bwr b = { 0, 0, 0, sizeof(rb), 44, (ulong)rb };
    wait_slot(100, 1);
    filter_binder_read(&b, 0, 100);
    CHECK(b.read_consumed == 0 && g_held_len == 44 && is_waiting(100));
    /* A second callback plus the sync reply: defer even in a mixed batch. */
    transaction(rb, data, 100, 9, TF_ONE_WAY_);
    clear(rb + 11, 11); rb[11] = BR_REPLY_; b.read_consumed = 88;
    filter_binder_read(&b, 0, 100);
    CHECK(b.read_consumed == 44 && rb[0] == BR_REPLY_ && g_held_len == 88 && !is_waiting(100));
    /* Preserve a preexisting read prefix and replay in FIFO order. */
    clear(rb, 300); rb[0] = 0x720c; b.read_consumed = 4; b.read_size = 48;
    filter_binder_read(&b, 4, 200);
    CHECK(b.read_consumed == 4 && g_held_len == 88);
    b.read_size = sizeof(rb); filter_binder_read(&b, 4, 200);
    CHECK(b.read_consumed == 92 && rb[0] == 0x720c && rb[4] == 8 && rb[15] == 9 && g_held_len == 0);
    CHECK(rb[6] == 123 && rb[7] == 1000 && rb[10] == (unsigned)(ulong)data);
    transaction(rb, data, 100, 8, TF_ONE_WAY_); b.read_consumed = 44;
    wait_slot(100, 1); filter_binder_read(&b, 0, 100);
    CHECK(g_held_len == 44); b.read_consumed = 0;
    /* Invalid fd proves a queued callback is drained before a blocking ioctl. */
    CHECK(ioctl(-1, BINDER_WRITE_READ_, &b) == 0 && b.read_consumed == 44 && g_held_len == 0);
    /* Never defer synchronous methods or other interfaces. */
    wait_slot(100, 1); transaction(rb, data, 100, 1, 0); b.read_consumed = 44;
    filter_binder_read(&b, 0, 100); CHECK(b.read_consumed == 44 && g_held_len == 0);
    data[1] = 43; rb[4] = TF_ONE_WAY_;
    filter_binder_read(&b, 0, 100); CHECK(b.read_consumed == 44);
    data[1] = 44; ((unsigned char *)data)[96] = 1;
    CHECK(!defer_async((unsigned char *)(rb + 1)));
    ((unsigned char *)data)[96] = 0; rb[7] = 97;
    CHECK(!defer_async((unsigned char *)(rb + 1)));
    rb[7] = 100; b.read_consumed = 20;
    filter_binder_read(&b, 0, 100); CHECK(b.read_consumed == 20 && g_held_len == 0);
    /* Retain existing audio exemptions, with a complete payload bound. */
    parcel(media, "android.media.IMediaPlayerClient", 32);
    transaction(rb, media, 80, 1, TF_ONE_WAY_); media[19] = 2;
    CHECK(is_player_notify((unsigned char *)(rb + 1)));
    rb[7] = 76; CHECK(!is_player_notify((unsigned char *)(rb + 1)));
    rb[7] = 80; media[19] = 1; CHECK(!is_player_notify((unsigned char *)(rb + 1)));
    media[19] = 4; CHECK(!is_player_notify((unsigned char *)(rb + 1)));
    /* Queue pressure must leave packets intact rather than drop them. */
    g_held_len = sizeof(g_held) - 20;
    transaction(rb, data, 100, 8, TF_ONE_WAY_); b.read_consumed = 44;
    filter_binder_read(&b, 0, 100);
    CHECK(b.read_consumed == 44 && g_held_len == sizeof(g_held) - 20);
    g_held_len = 0; wait_slot(100, 0);
    return 0;
}
__attribute__((used)) static void test_exit(void) {
    int result = test_main();
    if (!result) sys3(SYS_write, 1, (long)"ARM Binder/Keyguard smoke: PASS\n", 32);
    sys3(1, result, 0, 0); for (;;) {}
}
__attribute__((naked)) void _start(void) { __asm__ volatile("bl test_exit"); }
