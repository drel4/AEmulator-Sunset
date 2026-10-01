/* Legacy Android vibrator API -> emulator-owned host socket. No sysfs writes,
 * vendor patches, guest libc dependencies, or fd tracking. One acknowledged
 * request per connection keeps on/off ordering for sequential guest calls. */
#ifndef VIBRATION_SOCKET
#define VIBRATION_SOCKET "/dev/aemu_vibrator"
#endif
struct vb_addr { unsigned short family; char path[108]; };
struct vb_poll { int fd; short events, revents; };
static int vibration_request(unsigned op, unsigned milliseconds) {
    struct vb_addr addr;
    for (unsigned i = 0; i < sizeof(addr); i++) ((unsigned char *)&addr)[i] = 0;
    addr.family = 1;
    for (unsigned i = 0; i < sizeof(VIBRATION_SOCKET); i++) addr.path[i] = VIBRATION_SOCKET[i];
    long fd = sys3(281, 1, 1 | 02000000 /* CLOEXEC */ | 04000 /* NONBLOCK */, 0);
    if (fd < 0) return (int)fd;
    long r = sys3(283, fd, (long)&addr, sizeof(addr));
    if (r < 0) goto done;
    unsigned char packet[12];
    packet[0] = 'V'; packet[1] = 'I'; packet[2] = 'B'; packet[3] = '1';
    for (unsigned i = 0; i < 4; i++) {
        packet[4 + i] = op >> (8 * i);
        packet[8 + i] = milliseconds >> (8 * i);
    }
    r = sys4(289 /* send */, fd, (long)packet, sizeof(packet), 0x4000 /* MSG_NOSIGNAL */);
    if (r != sizeof(packet)) { if (r >= 0) r = -5; goto done; }
    struct vb_poll pollfd;
    pollfd.fd = (int)fd; pollfd.events = 1; pollfd.revents = 0;
    unsigned char reply[4]; unsigned got = 0;
    r = -110;
    for (unsigned attempts = 0; attempts < 10 && got < 4; attempts++) {
        long ready = sys3(168 /* poll */, (long)&pollfd, 1, 50);
        if (ready == -4 /* EINTR */ || ready == 0) continue;
        if (ready < 0) { r = ready; goto done; }
        long n = sys3(3 /* read */, fd, (long)(reply + got), 4 - got);
        if (n == -11 || n == -4) continue;
        if (n <= 0) { r = n < 0 ? n : -32; goto done; }
        got += (unsigned)n;
    }
    if (got == 4) {
        unsigned value = 0;
        for (unsigned i = 0; i < 4; i++) value |= (unsigned)reply[i] << (8 * i);
        r = (int)value;
    }
done:
    sys3(6, fd, 0, 0);
    return (int)r;
}
EXPORT int vibrator_exists(void) { return vibration_request(0, 0) > 0 ? 1 : 0; }
EXPORT int vibrator_on(unsigned milliseconds) {
    int r = vibration_request(1, milliseconds);
    return r < 0 ? fail(r) : r;
}
EXPORT int vibrator_off(void) {
    int r = vibration_request(2, 0);
    return r < 0 ? fail(r) : r;
}
