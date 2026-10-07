/* Sunset modification, 2026-10-07: synchronous legacy property writes.
 * Android 2.x-4.x's 250ms best-effort ACK can return before the host publishes
 * sys.settings_*_version, leaving a negative Settings cache entry valid.
 * The host accepts protocol 1 for every supported guest. Its socket close is
 * the completion ACK. Never report a timeout as a completed write.
 */
#ifndef AEMU_PROPERTY_SOCKET
#define AEMU_PROPERTY_SOCKET "/dev/socket/property_service"
#endif
#ifndef AEMU_PROPERTY_TIMEOUT_MS
#define AEMU_PROPERTY_TIMEOUT_MS 5000
#endif
static int property_now_ms(unsigned long *ms) {
    struct timespec_s now;
    long r = sys3(263 /* clock_gettime */, 1 /* CLOCK_MONOTONIC */, (long)&now, 0);
    if (r < 0) return -1;
    *ms = (unsigned long)now.tv_sec * 1000UL + (unsigned long)now.tv_nsec / 1000000UL;
    return 0;
}
EXPORT int __system_property_set(const char *key, const char *value) {
    struct { unsigned cmd; char name[32]; char value[92]; } msg;
    struct { unsigned short family; char path[108]; } addr;
    struct { int fd; short events; short revents; } pfd;
    unsigned k = 0, v = 0;
    if (!key) return fail(-22);
    if (!value) value = "";
    while (k < sizeof(msg.name) && key[k]) k++;
    while (v < sizeof(msg.value) && value[v]) v++;
    if (k >= sizeof(msg.name) || v >= sizeof(msg.value)) return fail(-22);
    for (unsigned i = 0; i < sizeof(msg); i++) ((char *)&msg)[i] = 0;
    for (unsigned i = 0; i < sizeof(addr); i++) ((char *)&addr)[i] = 0;
    msg.cmd = 1;
    for (unsigned i = 0; i < k; i++) msg.name[i] = key[i];
    for (unsigned i = 0; i < v; i++) msg.value[i] = value[i];
    addr.family = 1;
    for (unsigned i = 0; i < sizeof(AEMU_PROPERTY_SOCKET); i++) addr.path[i] = AEMU_PROPERTY_SOCKET[i];
    long fd = sys3(281 /* socket */, 1, 1 /* SOCK_STREAM */, 0);
    if (fd < 0) return fail(fd);
    /* Do not leak an in-flight property socket through exec. */
    long r;
    r = sys3(55 /* fcntl */, fd, 2 /* F_SETFD */, 1 /* FD_CLOEXEC */);
    if (r < 0) goto done;
    do { r = sys3(283 /* connect */, fd, (long)&addr, 2 + sizeof(AEMU_PROPERTY_SOCKET)); } while (r == -4);
    if (r < 0) goto done;
    unsigned sent = 0;
    while (sent < sizeof(msg)) {
        r = sys4(289 /* send */, fd, (long)((char *)&msg + sent), sizeof(msg) - sent, 0x4000 /* MSG_NOSIGNAL */);
        if (r == -4) continue;
        if (r <= 0) { if (r == 0) r = -32; goto done; }
        sent += (unsigned)r;
    }
    unsigned long start;
    if (property_now_ms(&start) < 0) { r = -5; goto done; }
    pfd.fd = fd; pfd.events = 0;
    for (;;) {
        unsigned long now;
        if (property_now_ms(&now) < 0) { r = -5; break; }
        unsigned long elapsed = now - start;
        if (elapsed >= AEMU_PROPERTY_TIMEOUT_MS) { r = -110; break; }
        pfd.revents = 0;
        r = sys3(168 /* poll */, (long)&pfd, 1, AEMU_PROPERTY_TIMEOUT_MS - elapsed);
        if (r == -4) continue;
        if (r == 0) { r = -110; break; }
        if (r < 0) break;
        if (pfd.revents & 0x10 /* POLLHUP */) { r = 0; break; }
        r = -5; break;
    }
done:
    sys3(6 /* close */, fd, 0, 0);
    return r < 0 ? fail(r) : 0;
}
