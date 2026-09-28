/*
 * librecshim.so — preloaded into a dynamic recovery (TWRP, OrangeFox…) running in recovery mode.
 *
 * mount() is not possible here, and qemu reports it as done. TWRP then checks "is it mounted" by comparing
 * st_dev of <mount point>/. and <mount point>/../. — equal, so it mounts again, recursively, until the stack
 * runs out. For the mount points in AEMU_MOUNTS ("/sdcard:/data:…") stat() of "<mp>/." reports another
 * device, so they look mounted (their folders are the system's own, set up by the host).
 * reboot()/__reboot()/android_reboot() go to the host through /dev/aemu_power, like in the system.
 */
#define EXPORT __attribute__((visibility("default")))

extern char **environ;
extern int *__errno(void);

__attribute__((naked, noinline)) static long sys3(long n, long a, long b, long c) {
    __asm__ volatile("push {r7}; mov r7, r0; mov r0, r1; mov r1, r2; mov r2, r3; svc #0; pop {r7}; bx lr");
}

static int fail(long r) { *__errno() = (int)-r; return -1; }

static const char *env(const char *key) {
    for (char **e = environ; e && *e; e++) {
        const char *a = *e, *k = key;
        while (*k && *a == *k) { a++; k++; }
        if (!*k && *a == '=') return a + 1;
    }
    return 0;
}

/* p is exactly "<mount point>/." for one of the AEMU_MOUNTS entries */
static int mount_dot(const char *p) {
    const char *list = env("AEMU_MOUNTS");
    if (!list || !p) return 0;
    int n = 0;
    while (p[n]) n++;
    if (n < 3 || p[n - 1] != '.' || p[n - 2] != '/') return 0;
    int len = n - 2;
    const char *s = list;
    while (*s) {
        const char *e = s;
        while (*e && *e != ':') e++;
        if (e - s == len) {
            int i = 0;
            while (i < len && s[i] == p[i]) i++;
            if (i == len) return 1;
        }
        s = *e ? e + 1 : e;
    }
    return 0;
}

EXPORT int stat(const char *path, void *st) {
    long r = sys3(195 /* stat64 */, (long)path, (long)st, 0);
    if (r < 0) return fail(r);
    if (mount_dot(path)) *(unsigned long long *)st ^= 0x5a5aULL;   /* st_dev: "another filesystem" */
    return 0;
}
EXPORT int stat64(const char *path, void *st) { return stat(path, st); }

static void power_request(const char *what, const char *arg) {
    char buf[96]; int n = 0;
    while (*what && n < 40) buf[n++] = *what++;
    if (arg && *arg) { buf[n++] = ','; while (*arg && n < 90) buf[n++] = *arg++; }
    long fd = sys3(5 /* open */, (long)"/dev/aemu_power", 0x241 /* O_WRONLY|O_CREAT|O_TRUNC */, 0666);
    if (fd >= 0) { sys3(4 /* write */, fd, (long)buf, n); sys3(6 /* close */, fd, 0, 0); }
}
EXPORT int reboot(int cmd) {
    power_request(cmd == 0x4321FEDC || cmd == (int)0xCDEF0123 ? "shutdown" : "reboot", 0);
    return 0;
}
EXPORT int __reboot(int m1, int m2, int cmd, void *arg) {
    (void)m1; (void)m2;
    if (cmd == (int)0xA1B2C3D4) power_request("reboot", (const char *)arg); else reboot(cmd);
    return 0;
}
EXPORT int android_reboot(int cmd, int flags, char *arg) {
    (void)flags;
    power_request(cmd == (int)0xDEAD0002 ? "shutdown" : "reboot", cmd == (int)0xDEAD0003 ? arg : 0);
    return 0;
}

/* the app sandbox kills the process on umount (seccomp); partitions are the host's folders anyway */
EXPORT int mount(const char *src, const char *target, const char *type, unsigned long flags, const void *data) {
    (void)src; (void)target; (void)type; (void)flags; (void)data; return 0;
}
EXPORT int umount(const char *target) { (void)target; return 0; }
EXPORT int umount2(const char *target, int flags) { (void)target; (void)flags; return 0; }
