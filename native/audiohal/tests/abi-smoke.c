/* Production HAL through real ARM callback offsets, with deterministic IO. */
#include "../audio_hal.c"
static int test_errno, opens, closes, open_flags;
static unsigned char arena[8192] __attribute__((aligned(8)));
static unsigned used;
int *__errno(void) { return &test_errno; }
void *calloc(unsigned long n, unsigned long size) {
    if (n > sizeof(arena) || size > sizeof(arena)) return 0;
    unsigned count = n * size, start = used;
    if (count > sizeof(arena) - used) return 0;
    used = (used + count + 7) & ~7;
    if (used > sizeof(arena)) return 0;
    for (unsigned i = 0; i < count; i++) arena[start + i] = 0;
    return arena + start;
}
void free(void *p) { (void)p; }
int open(const char *p, int flags, ...) { (void)p; opens++; open_flags = flags; return 7; }
long write(int fd, const void *buf, unsigned long n) { (void)buf; return fd == 7 ? (long)n : -1; }
int close(int fd) { if (fd == 7) closes++; return 0; }
int usleep(unsigned long us) { (void)us; return 0; }
__attribute__((naked)) static long raw(long n, long a, long b, long c) {
    __asm__ volatile("push {r7}; mov r7,r0; mov r0,r1; mov r1,r2; mov r2,r3; svc #0; pop {r7}; bx lr");
}
#define CHECK(c) do { if (!(c)) return __LINE__; } while (0)
_Static_assert(offsetof(struct audio_stream_out, write) == 0x40, "PCM ABI");
#ifdef AEMU_DIRECTTRACK
_Static_assert(offsetof(struct audio_stream_out, start) == 0x48, "CAF start");
_Static_assert(offsetof(struct audio_stream_out, stop) == 0x4c, "CAF stop");
_Static_assert(offsetof(struct audio_stream_out, get_next_write_timestamp) == 0x50, "ZR next timestamp");
_Static_assert(offsetof(struct audio_stream_out, get_presentation_position) == 0x68, "ZR presentation");
#else
_Static_assert(offsetof(struct audio_stream_out, get_next_write_timestamp) == 0x48, "AOSP timestamp");
_Static_assert(offsetof(struct audio_stream_out, get_presentation_position) == 0x60, "AOSP presentation");
#endif
_Static_assert(offsetof(struct audio_hw_device, open_output_stream) == 0x6c, "standard device ABI unchanged");
static int test_main(void) {
    struct hw_device *hw = 0;
    CHECK(hal_open(&HMI, "wrong", &hw) == -EINVAL);
    CHECK(hal_open(&HMI, "audio_hw_if", &hw) == 0 && hw);
    struct audio_hw_device *dev = (struct audio_hw_device *)hw;
    unsigned char guard[3] = { 0x51, 0xff, 0x73 };
    CHECK(dev->get_mic_mute(dev, guard + 1) == 0);
    CHECK(guard[0] == 0x51 && guard[1] == 0 && guard[2] == 0x73);
    struct audio_config config = { 44100, 1, 99 };
    struct audio_stream_out *out = 0;
    CHECK(dev->open_output_stream(dev, 1, 2, 2, &config, &out) == 0 && out);
    CHECK(config.sample_rate == RATE && config.channel_mask == 3 && config.format == 1);
    CHECK(out->common.get_sample_rate(&out->common) == RATE);
    long (*pcm)(struct audio_stream_out *, const void *, size_t) = *(void **)((char *)out + 0x40);
    unsigned buffer[1024];
    CHECK(pcm(out, buffer, sizeof(buffer)) == sizeof(buffer));
    CHECK(opens == 1 && (open_flags & O_RDWR) && (open_flags & O_NONBLOCK));
    uint32_t frames = 0;
    CHECK(out->get_render_position(out, &frames) == 0 && frames == 1024);
#ifdef AEMU_DIRECTTRACK
    // Reproduce stock libnbaio's loads; none may land in fd/device/frame data.
    int (*next)(const struct audio_stream_out *, int64_t *) = *(void **)((char *)out + 0x50);
    CHECK(next(out, 0) == -EINVAL);
    CHECK(*(void **)((char *)out + 0x68) == 0); // stock returns -ENOSYS, not PC=2
    CHECK(out->start(out) == -ENOSYS && out->stop(out) == -ENOSYS);
#else
    CHECK(out->get_next_write_timestamp(out, 0) == -EINVAL);
    CHECK(out->get_presentation_position == 0);
#endif
    CHECK(out->common.standby(&out->common) == 0 && closes == 1);
    dev->close_output_stream(dev, out);
    CHECK(hw->close(hw) == 0);
    return 0;
}
void _start(void) {
    int result = test_main();
    const char *message = result ? "audio ABI smoke FAILED\n" : "audio ABI smoke OK\n";
    raw(4, 1, (long)message, result ? 23 : 19);
    raw(1, result, 0, 0);
    for (;;) {}
}
