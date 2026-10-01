/* Offline Qualcomm netmgr multicast endpoint. Never accesses host netlink.
 * Only used when QEMU cannot create NETLINK_GENERIC/SOCK_RAW. No modem events
 * or fabricated link-up replies: the receive fd remains idle and pollable.
 * Kernel socket names, not a process fd table, identify dup/fork/close safely. */
struct nm_addr { unsigned short family, pad; unsigned pid, groups; };
struct nm_un { unsigned short family; unsigned char path[28]; };
static unsigned nm_sequence;
struct nm_iov { void *base; unsigned size; };
struct nm_msg {
    void *name; unsigned namelen; struct nm_iov *iov; unsigned iovlen;
    void *control; unsigned controllen; int flags;
};
static void nm_diag(const char *stage, long error) {
    char out[128]; unsigned n = 0;
    const char *prefix = "aemushim netmgr: ";
    while (*prefix) out[n++] = *prefix++;
    while (*stage && n < 100) out[n++] = *stage++;
    out[n++] = ' ';
    if (error < 0) out[n++] = '-';
    unsigned v = error < 0 ? (unsigned)-error : (unsigned)error;
    char digits[12]; unsigned k = 0;
    do { digits[k++] = '0' + v % 10; v /= 10; } while (v);
    while (k) out[n++] = digits[--k];
    out[n++] = '\n'; sys3(4, 2, (long)out, n);
}
static int nm_identity(int fd, struct nm_un *addr) {
    unsigned size = sizeof(*addr);
    if (sys3(286, fd, (long)addr, (long)&size) < 0 || size != sizeof(*addr) || addr->family != 1) return 0;
    const char *tag = "aemu-netmgr-v1";
    if (addr->path[0] != 0) return 0;
    for (unsigned i = 0; i < 14; i++) if (addr->path[i + 1] != (unsigned char)tag[i]) return 0;
    return 1;
}
static unsigned nm_pid(void) { return (unsigned)sys3(20, 0, 0, 0) & 0x7fffffff; }
EXPORT int socket(int domain, int type, int protocol) {
    long r = sys3(281, domain, type, protocol);
#ifdef NETMGR_TEST_FORCE_FALLBACK
    if (domain == 16 && (type & 15) == 3 && protocol == 16) {
        if (r >= 0) sys3(6, r, 0, 0);
        r = -93;
    }
#endif
    if (r >= 0) return (int)r;
    if (domain != 16 || (type & 15) != 3 || protocol != 16) return fail(r);
    if (r != -93 && r != -97) {
        nm_diag("socket failed (no fallback for this error), errno", r);
        return fail(r);
    }
    nm_diag("socket unavailable; using offline endpoint, errno", r);
    r = sys3(281, 1, 2 | (type & (04000 | 02000000)), 0);
    if (r < 0) { nm_diag("offline socket failed, errno", r); return fail(r); }
    int fd = (int)r;
    struct nm_un addr;
    addr.family = 1;
    for (unsigned i = 0; i < sizeof(addr.path); i++) addr.path[i] = 0;
    const char *tag = "aemu-netmgr-v1";
    for (unsigned i = 0; i < 14; i++) addr.path[i + 1] = tag[i];
    unsigned pid = nm_pid();
    unsigned sequence = __sync_fetch_and_add(&nm_sequence, 1);
    for (unsigned i = 0; i < 4; i++) {
        addr.path[16 + i] = pid >> (i * 8);
        addr.path[20 + i] = (unsigned)fd >> (i * 8);
        addr.path[24 + i] = sequence >> (i * 8);
    }
    r = sys3(282, fd, (long)&addr, sizeof(addr));
    if (r < 0) { sys3(6, fd, 0, 0); nm_diag("offline bind failed, errno", r); return fail(r); }
    return fd;
}
EXPORT int bind(int fd, const void *address, unsigned size) {
    struct nm_un own;
    if (!nm_identity(fd, &own)) {
        long r = sys3(282, fd, (long)address, size);
        return r < 0 ? fail(r) : (int)r;
    }
    if (!address) return fail(-14);
    if (size != sizeof(struct nm_addr)) return fail(-22);
    const struct nm_addr *addr = address;
    if (addr->family != 16 || addr->groups != 0x40000000 || addr->pid != nm_pid()) {
        nm_diag("unsupported bind (only vendor group 31), errno", -93);
        return fail(-93);
    }
    struct nm_un peer; unsigned len = sizeof(peer);
    if (sys3(287, fd, (long)&peer, (long)&len) == 0) return fail(-22);
    long r = sys3(283, fd, (long)&own, sizeof(own));
    if (r < 0) return fail(r);
    nm_diag("vendor group 31 bound offline", 0);
    return 0;
}
static int nm_getsockname(int fd, void *address, unsigned *size) {
    struct nm_un own;
    if (!nm_identity(fd, &own)) return -4096;
    if (!address || !size) return fail(-14);
    struct nm_un peer; unsigned plen = sizeof(peer);
    struct nm_addr addr;
    addr.family = 16; addr.pad = 0;
    addr.pid = 0;
    for (unsigned i = 0; i < 4; i++) addr.pid |= (unsigned)own.path[16 + i] << (i * 8);
    addr.groups = sys3(287, fd, (long)&peer, (long)&plen) == 0 ? 0x40000000 : 0;
    unsigned n = *size < sizeof(addr) ? *size : sizeof(addr);
    for (unsigned i = 0; i < n; i++) ((unsigned char *)address)[i] = ((unsigned char *)&addr)[i];
    *size = sizeof(addr); return 0;
}
EXPORT long sendmsg(int fd, const struct nm_msg *msg, int flags) {
    struct nm_un own;
    if (!nm_identity(fd, &own)) {
        long r = sys3(296, fd, (long)msg, flags);
        return r < 0 ? fail(r) : r;
    }
    if (!msg || !msg->name || (msg->iovlen && !msg->iov)) return fail(-14);
    struct nm_un peer; unsigned plen = sizeof(peer);
    if (sys3(287, fd, (long)&peer, (long)&plen) < 0) return fail(-107);
    if (msg->namelen != sizeof(struct nm_addr) || msg->iovlen > 1024) return fail(-22);
    const struct nm_addr *addr = msg->name;
    if (addr->family != 16 || addr->pid != 0 || addr->groups != 0x40000000) return fail(-93);
    unsigned total = 0;
    for (unsigned i = 0; i < msg->iovlen; i++) {
        if (msg->iov[i].size && !msg->iov[i].base) return fail(-14);
        if (msg->iov[i].size > 0x7fffffff - total) return fail(-90);
        total += msg->iov[i].size;
    }
    /* Multicast send with no modem peer succeeds; never echo a ping as an event. */
    return total;
}
