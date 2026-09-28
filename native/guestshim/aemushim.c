/*
 * libaemushim.so — гостевая прослойка AEmulator (32-битный ARM, подгружается через LD_PRELOAD
 * в каждый процесс гостя вместе с libashmemshim.so).
 *
 * Прошивка работает внутри приложения Android, где seccomp запрещает часть системных вызовов:
 * mount, umount, swapon, reboot, settimeofday… Системные службы старых прошивок (например,
 * system_server Samsung монтирует /efs сам) получают за такой вызов SIGSYS и падают.
 * Здесь эти функции libc подменены безопасными заглушками: «успех» без обращения к ядру.
 *
 * Второе: qemu стенда умеет запускать только ELF — execve() скрипта (#!/system/bin/sh: am, pm, input,
 * monkey, svc…) заканчивается «Exec format error» уже в новом процессе, и гость не узнаёт об ошибке.
 * execve/execv/execvp здесь сами разбирают строку #! и запускают интерпретатор, как это делает ядро.
 *
 * Библиотека собирается с -nostdlib и зовёт ядро напрямую; из libc гостя берёт только environ,
 * поэтому одинаково грузится в bionic Android 2.3–6.0.
 */

#define EXPORT __attribute__((visibility("default")))

typedef unsigned long ulong;
struct timeval_s { long tv_sec; long tv_usec; };
struct timespec_s { long tv_sec; long tv_nsec; };

/* монтирование: раздел «уже смонтирован» */
EXPORT int mount(const char *src, const char *target, const char *type, ulong flags, const void *data) {
    (void)src; (void)target; (void)type; (void)flags; (void)data;
    return 0;
}
EXPORT int umount(const char *target) { (void)target; return 0; }
EXPORT int umount2(const char *target, int flags) { (void)target; (void)flags; return 0; }

/* подкачка, учёт процессов, смена корня */
EXPORT int swapon(const char *path, int flags) { (void)path; (void)flags; return 0; }
EXPORT int swapoff(const char *path) { (void)path; return 0; }
EXPORT int acct(const char *file) { (void)file; return 0; }

/* время ставит телефон: попытки гостя поменять часы игнорируем */
EXPORT int settimeofday(const struct timeval_s *tv, const void *tz) { (void)tv; (void)tz; return 0; }
EXPORT int clock_settime(int clk, const struct timespec_s *tp) { (void)clk; (void)tp; return 0; }
EXPORT int stime(const long *t) { (void)t; return 0; }

/* Reboot / power off: the host restarts or stops the VM. The request goes to /dev/aemu_power
 * ("reboot", "reboot,recovery", "shutdown"), which the host watches. */
static long sys3(long n, long a, long b, long c);
static void power_request(const char *what, const char *arg) {
    char buf[96]; int n = 0;
    while (*what && n < 40) buf[n++] = *what++;
    if (arg && *arg) { buf[n++] = ','; while (*arg && n < 90) buf[n++] = *arg++; }
    long fd = sys3(5 /* open */, (long)"/dev/aemu_power", 0x241 /* O_WRONLY|O_CREAT|O_TRUNC */, 0666);
    if (fd >= 0) { sys3(4 /* write */, fd, (long)buf, n); sys3(6 /* close */, fd, 0, 0); }
}
EXPORT int reboot(int cmd) {
    power_request(cmd == 0x4321FEDC /* POWER_OFF */ || cmd == (int)0xCDEF0123 /* HALT */ ? "shutdown" : "reboot", 0);
    return 0;
}
EXPORT int __reboot(int m1, int m2, int cmd, void *arg) {
    (void)m1; (void)m2;
    if (cmd == (int)0xA1B2C3D4 /* RESTART2 */) power_request("reboot", (const char *)arg);
    else reboot(cmd);
    return 0;
}
EXPORT int android_reboot(int cmd, int flags, char *arg) {
    (void)flags;
    power_request(cmd == (int)0xDEAD0002 ? "shutdown" : "reboot", cmd == (int)0xDEAD0003 ? arg : 0);
    return 0;
}

/* модули ядра */
EXPORT int init_module(void *img, ulong len, const char *params) { (void)img; (void)len; (void)params; return 0; }
EXPORT int delete_module(const char *name, int flags) { (void)name; (void)flags; return 0; }
EXPORT int klogctl(int type, char *buf, int len) { (void)type; (void)buf; (void)len; return 0; }

/* ------------------------------------------------------------------ запуск скриптов */

extern char **environ;

/* системный вызов с тремя аргументами; r7 в Thumb занят под кадр — сохраняем его сами */
__attribute__((naked, noinline)) static long sys3(long n, long a, long b, long c) {
    __asm__ volatile(
        "push {r7}; mov r7, r0; mov r0, r1; mov r1, r2; mov r2, r3; svc #0; pop {r7}; bx lr");
}
#define SYS_read 3
#define SYS_write 4
#define SYS_open 5
#define SYS_close 6
#define SYS_execve 11
#define SYS_access 33
#define ENOENT 2
#define E2BIG 7

extern int *__errno(void);

static int fail(long r) { *__errno() = (int)-r; return -1; }

static long raw_execve(const char *path, char *const argv[], char *const envp[]) {
    return sys3(SYS_execve, (long)path, (long)argv, (long)envp);
}

#define MAXARGS 512

EXPORT int execve(const char *path, char *const argv[], char *const envp[]) {
    char head[256];
    long fd = sys3(SYS_open, (long)path, 0 /* O_RDONLY */, 0);
    if (fd >= 0) {
        long n = sys3(SYS_read, fd, (long)head, sizeof(head) - 1);
        sys3(SYS_close, fd, 0, 0);
        if (n > 2 && head[0] == '#' && head[1] == '!') {
            head[n] = 0;
            /* строка интерпретатора: #!/путь [один аргумент] */
            char *p = head + 2, *interp, *arg = 0;
            while (*p == ' ' || *p == 9) p++;
            interp = p;
            while (*p && *p != ' ' && *p != 9 && *p != 10 && *p != 13) p++;
            if (*p == ' ' || *p == 9) {
                *p++ = 0;
                while (*p == ' ' || *p == 9) p++;
                if (*p && *p != 10 && *p != 13) {
                    arg = p;
                    while (*p && *p != 10 && *p != 13) p++;
                    while (p > arg && (p[-1] == ' ' || p[-1] == 9)) p--;
                }
            }
            *p = 0;
            if (*interp) {
                char *nargv[MAXARGS];
                int k = 0;
                nargv[k++] = interp;
                if (arg) nargv[k++] = arg;
                nargv[k++] = (char *)path;
                if (argv && argv[0]) for (int i = 1; argv[i]; i++) {
                    if (k >= MAXARGS - 1) return fail(-E2BIG);
                    nargv[k++] = argv[i];
                }
                nargv[k] = 0;
                return fail(raw_execve(interp, nargv, envp));
            }
        }
    }
    return fail(raw_execve(path, argv, envp));
}

EXPORT int execv(const char *path, char *const argv[]) { return execve(path, argv, environ); }

EXPORT int execvp(const char *file, char *const argv[]) {
    int slash = 0;
    for (const char *c = file; *c; c++) if (*c == '/') { slash = 1; break; }
    if (slash) return execve(file, argv, environ);
    const char *path = 0;
    for (char **e = environ; e && *e; e++) {
        const char *v = *e;
        if (v[0] == 'P' && v[1] == 'A' && v[2] == 'T' && v[3] == 'H' && v[4] == '=') { path = v + 5; break; }
    }
    if (!path) path = "/system/bin:/system/xbin:/vendor/bin:/sbin";
    char buf[512];
    int last = -ENOENT;
    while (1) {
        const char *end = path;
        while (*end && *end != ':') end++;
        int len = (int)(end - path), flen = 0;
        while (file[flen]) flen++;
        if (len > 0 && len + 1 + flen < (int)sizeof(buf)) {
            for (int i = 0; i < len; i++) buf[i] = path[i];
            buf[len] = '/';
            for (int i = 0; i <= flen; i++) buf[len + 1 + i] = file[i];
            if (sys3(SYS_access, (long)buf, 1 /* X_OK */, 0) == 0) {
                execve(buf, argv, environ);
                last = -*__errno();
            }
        }
        if (!*end) break;
        path = end + 1;
    }
    return fail(last);
}

/* ------------------------------------------------------------------ учёт трафика (xt_qtaguid) */
/*
 * NetworkManagementService включает учёт трафика, только если есть /proc/net/xt_qtaguid/ctrl; без него
 * NetworkStatsService бросает «Bandwidth module disabled» и падают менеджеры трафика (MIUI «Безопасность»,
 * «Использование данных»). /proc qemu не подменяет — подменяем здесь: ctrl → /dev/null (метки сокетов
 * пишутся туда), таблицы статистики → пустые таблицы с заголовками в /data/.aemu_qtaguid (их кладёт движок).
 */
static const char QT[] = "/proc/net/xt_qtaguid/";
static const char *qt_redirect(const char *path, char *buf, int n) {
    if (!path) return path;
    /* MTK: ActivityManager пишет отметки загрузки в /proc/bootprof на каждый запуск процесса;
       хост не пускает, и каждый раз в журнал летит исключение со стеком */
    static const char BP[] = "/proc/bootprof";
    int b = 0;
    while (BP[b] && path[b] == BP[b]) b++;
    if (!BP[b] && !path[b]) return "/dev/null";
    int i = 0;
    while (QT[i] && path[i] == QT[i]) i++;
    if (QT[i]) {
        /* сам каталог */
        if (!path[i] && i == (int)sizeof(QT) - 2) return "/data/.aemu_qtaguid";
        return path;
    }
    const char *name = path + i;
    if (name[0] == 'c' && name[1] == 't' && name[2] == 'r' && name[3] == 'l' && !name[4]) return "/dev/null";
    const char *pre = "/data/.aemu_qtaguid/";
    int k = 0;
    while (pre[k] && k < n - 1) { buf[k] = pre[k]; k++; }
    for (int j = 0; name[j] && k < n - 1; j++) buf[k++] = name[j];
    buf[k] = 0;
    return buf;
}

EXPORT int access(const char *path, int mode) {
    char b[128];
    long r = sys3(SYS_access, (long)qt_redirect(path, b, sizeof(b)), mode, 0);
    return r < 0 ? fail(r) : 0;
}

/* ------------------------------------------------------------------ важность процессов (oom_adj) */
/*
 * ActivityManager до 4.3 пишет важность процессов в /proc/<pid>/oom_adj, хост это запрещает, и ядерный
 * lowmemorykiller всё равно не про нас. Отдаём вместо файла FIFO /dev/aemu_oom с заголовком
 * «\n<pid> a » (или « s » для oom_score_adj): следом ActivityManager сам допишет число, а хост (GuestLmk)
 * убивает по этим числам фоновые процессы, когда памяти мало. Нет читателя — пишем в /dev/null.
 */
static int oom_open(const char *path) {
    static const char P[] = "/proc/";
    int i = 0;
    while (P[i] && path[i] == P[i]) i++;
    if (P[i]) return -1;
    const char *pid = path + i;
    int n = 0;
    while (pid[n] >= '0' && pid[n] <= '9') n++;
    if (!n || n > 10 || pid[n] != '/') return -1;
    const char *f = pid + n + 1;
    static const char A[] = "oom_adj", S[] = "oom_score_adj";
    int ka = 0, ks = 0;
    while (A[ka] && f[ka] == A[ka]) ka++;
    while (S[ks] && f[ks] == S[ks]) ks++;
    char kind;
    if (!A[ka] && !f[ka]) kind = 'a';
    else if (!S[ks] && !f[ks]) kind = 's';
    else return -1;
    long fd = sys3(SYS_open, (long)"/dev/aemu_oom", 01 /* O_WRONLY */ | 04000 /* O_NONBLOCK */ | 02000000 /* O_CLOEXEC */, 0);
    if (fd < 0) fd = sys3(SYS_open, (long)"/dev/null", 01 | 02000000, 0);
    if (fd < 0) return -1;
    char h[16];
    int k = 0;
    h[k++] = '\n';
    for (int j = 0; j < n; j++) h[k++] = pid[j];
    h[k++] = ' '; h[k++] = kind; h[k++] = ' ';
    sys3(SYS_write, fd, (long)h, k);
    return (int)fd;
}

EXPORT int open(const char *path, int flags, ...) {
    char b[128];
    int mode = 0;
    if (flags & 0100 /* O_CREAT */) {
        __builtin_va_list ap; __builtin_va_start(ap, flags); mode = __builtin_va_arg(ap, int); __builtin_va_end(ap);
    }
    if (path && (flags & 3) && path[0] == '/' && path[1] == 'p') {
        int fd = oom_open(path);
        if (fd >= 0) return fd;
    }
    long r = sys3(SYS_open, (long)qt_redirect(path, b, sizeof(b)), flags | 0400000 /* O_LARGEFILE */, mode);
    return r < 0 ? fail(r) : (int)r;
}

EXPORT int __open_2(const char *path, int flags) { return open(path, flags); }

/*
 * Пустые «запасные» виртуальные методы VectorImpl/SortedVectorImpl из libutils 4.0–4.3.
 * В libutils части прошивок (MediaTek 4.4 и др.) их выбросили, и библиотеки, собранные под AOSP
 * (политика звука движка), не загружаются: «cannot locate symbol reservedVectorImpl1».
 * В AOSP они пустые и никогда не вызываются — даём их всем процессам через LD_PRELOAD.
 */
#define RESERVED(n) EXPORT void _ZN7android10VectorImpl19reservedVectorImpl##n##Ev(void *self) { (void)self; } \
    EXPORT void _ZN7android16SortedVectorImpl25reservedSortedVectorImpl##n##Ev(void *self) { (void)self; }
RESERVED(1) RESERVED(2) RESERVED(3) RESERVED(4) RESERVED(5) RESERVED(6) RESERVED(7) RESERVED(8)

/* ------------------------------------------------------------------ sigsuspend
 *
 * qemu of the stand mis-emulates rt_sigsuspend: the call returns at once without delivering the
 * pending (host-blocked) signal. A shell waiting for its child blocks SIGCHLD, calls sigsuspend
 * and spins at 100% CPU forever — every `sh -c` that runs a program never returns (MIUI firewall,
 * am/pm from the emulator menu). Here the mask is lifted with rt_sigprocmask (qemu delivers
 * pending signals after that syscall), we sleep briefly, restore the mask and report EINTR, which
 * is what a real sigsuspend returns once a handler has run.
 */
__attribute__((naked, noinline)) static long sys4(long n, long a, long b, long c, long d) {
    __asm__ volatile(
        "push {r7}; mov r7, r0; mov r0, r1; mov r1, r2; mov r2, r3; ldr r3, [sp, #4]; svc #0; pop {r7}; bx lr");
}
#define SYS_nanosleep 162
#define SYS_rt_sigprocmask 175
#define SIG_SETMASK 2
#define EINTR 4

EXPORT int sigsuspend(const unsigned long *mask) {
    unsigned long want[2] = { mask ? mask[0] : 0, 0 }, old[2] = { 0, 0 };
    sys4(SYS_rt_sigprocmask, SIG_SETMASK, (long)want, (long)old, 8);
    struct timespec_s ts = { 0, 5 * 1000 * 1000 };
    sys3(SYS_nanosleep, (long)&ts, 0, 0);
    sys4(SYS_rt_sigprocmask, SIG_SETMASK, (long)old, 0, 8);
    *__errno() = EINTR;
    return -1;
}

/*
 * Fault handlers that resume at a Thumb function (ART's implicit null / stack-overflow / suspend checks:
 * the SIGSEGV handler sets uc->arm_pc = art_quick_throw_null_pointer_exception, an address with bit 0 set).
 * A real CPU ignores PC bit 0 on the exception return, qemu keeps it and aborts translating an odd Thumb pc.
 * The fault signals' handlers are wrapped: after the guest's handler returns, an odd pc becomes even with the
 * Thumb bit set in cpsr. Everything else about sigaction stays as the kernel does it.
 */
#define SYS_rt_sigaction 174
#define SA_SIGINFO_ 4
#define SA_RESTORER_ 0x04000000
struct bionic_sigaction { void *handler; unsigned long mask; int flags; void (*restorer)(void); };
struct kernel_sigaction { void *handler; unsigned long flags; void (*restorer)(void); unsigned long mask[2]; };
static struct bionic_sigaction guest_act[32];

static int wrapped(int sig) { return sig == 4 /* ILL */ || sig == 5 /* TRAP */ || sig == 7 /* BUS */ || sig == 8 /* FPE */ || sig == 11 /* SEGV */; }

static void fault_trampoline(int sig, void *info, void *uc) {
    struct bionic_sigaction *a = &guest_act[sig & 31];
    if (a->flags & SA_SIGINFO_) ((void (*)(int, void *, void *))a->handler)(sig, info, uc);
    else ((void (*)(int))a->handler)(sig);
    /* ucontext: uc_flags, uc_link, uc_stack(3) | trap_no, error_code, oldmask, r0..r10, fp, ip, sp, lr, pc, cpsr */
    ulong *pc = (ulong *)((char *)uc + 20 + 4 * 18);
    ulong *cpsr = pc + 1;
    if (*pc & 1) { *pc &= ~1UL; *cpsr |= 0x20; }
}

EXPORT int sigaction(int sig, const struct bionic_sigaction *act, struct bionic_sigaction *old) {
    struct kernel_sigaction k, ko;
    struct kernel_sigaction *kp = 0;
    int wrap = wrapped(sig) && act && act->handler != (void *)0 && act->handler != (void *)1;
    if (act) {
        k.handler = act->handler; k.flags = (unsigned long)act->flags; k.restorer = act->restorer;
        k.mask[0] = act->mask; k.mask[1] = 0;
        if (wrap) { k.handler = (void *)fault_trampoline; k.flags |= SA_SIGINFO_; }
        kp = &k;
    }
    long r = sys4(SYS_rt_sigaction, sig, (long)kp, (long)&ko, 8);
    if (r < 0) return fail(r);
    if (old) {
        if (ko.handler == (void *)fault_trampoline) *old = guest_act[sig & 31];
        else { old->handler = ko.handler; old->mask = ko.mask[0]; old->flags = (int)ko.flags; old->restorer = ko.restorer; }
    }
    if (wrap) guest_act[sig & 31] = *act;
    return 0;
}


/*
 * libbacktrace (ART thread dumps on SIGQUIT, ANR traces) unwinds each thread by sending it signal 33 and
 * waiting on a futex for the handler; under qemu every wait runs into its timeout, a dump of system_server
 * takes minutes and the watchdogs kill the system. That request fails at once instead: dumps keep Java stacks.
 */
#define SYS_tgkill 268
EXPORT int tgkill(int tgid, int tid, int sig) {
    if (sig == 33 /* libbacktrace THREAD_SIGNAL, __SIGRTMIN+1 */) { *__errno() = 22; return -1; }
    long r = sys3(SYS_tgkill, tgid, tid, sig);
    return r < 0 ? fail(r) : 0;
}

/*
 * binder ordering. The stand's binderd hands a oneway transaction to a thread that is itself waiting for the
 * reply of a synchronous call (the kernel driver never does: such a thread only takes work from its own call
 * chain). An app's main thread blocked in attachApplication then receives scheduleCreateService/LaunchActivity
 * while bindApplication, sent earlier, went to a pool thread — whichever posts to the main looper first wins,
 * and a lost race kills the app ("Instrumentation.onException on a null object"). A oneway transaction that
 * arrives nested in a pending call is held back briefly so the earlier one is queued first.
 */
#define BINDER_WRITE_READ_ 0xc0186201
#define BC_TRANSACTION_ 0x40286300
#define BR_TRANSACTION_ 0x80287202
#define BR_REPLY_ 0x80287203
#define TF_ONE_WAY_ 1
#define SYS_ioctl 54
#define SYS_gettid 224
struct bwr { long write_size, write_consumed; ulong write_buffer; long read_size, read_consumed; ulong read_buffer; };
static volatile int waiting_tid[64];

static int wait_slot(int tid, int add) {
    for (int k = 0; k < 64; k++) if (waiting_tid[k] == tid) { if (!add) waiting_tid[k] = 0; return 1; }
    if (add) for (int k = 0; k < 64; k++) if (__sync_bool_compare_and_swap(&waiting_tid[k], 0, tid)) return 1;
    return 0;
}

static int is_waiting(int tid) { for (int k = 0; k < 64; k++) if (waiting_tid[k] == tid) return 1; return 0; }

EXPORT int ioctl(int fd, int req, void *arg) {
    if ((unsigned)req != BINDER_WRITE_READ_ || !arg) {
        long r = sys3(SYS_ioctl, fd, req, (long)arg);
        return r < 0 ? fail(r) : (int)r;
    }
    struct bwr *b = (struct bwr *)arg;
    int tid = (int)sys3(SYS_gettid, 0, 0, 0);
    // a synchronous BC_TRANSACTION in this write: the thread now waits for its reply
    for (long p = b->write_consumed; p + 4 <= b->write_size;) {
        unsigned cmd = *(unsigned *)(b->write_buffer + p);
        unsigned sz = (cmd >> 16) & 0x3fff;
        if (cmd == BC_TRANSACTION_ && p + 4 + 16 <= b->write_size && !(*(unsigned *)(b->write_buffer + p + 4 + 12) & TF_ONE_WAY_))
            wait_slot(tid, 1);
        p += 4 + sz;
    }
    long start = b->read_consumed;
    long r = sys3(SYS_ioctl, fd, req, (long)arg);
    if (r < 0) return fail(r);
    int nested_oneway = 0, replied = 0;
    for (long p = start; p + 4 <= b->read_consumed;) {
        unsigned cmd = *(unsigned *)(b->read_buffer + p);
        unsigned sz = (cmd >> 16) & 0x3fff;
        if (cmd == BR_REPLY_ || cmd == 0x7205 /* BR_DEAD_REPLY */ || cmd == 0x7211 /* BR_FAILED_REPLY */) replied = 1;
        else if (cmd == BR_TRANSACTION_ && p + 4 + 16 <= b->read_consumed && (*(unsigned *)(b->read_buffer + p + 4 + 12) & TF_ONE_WAY_))
            nested_oneway = 1;
        p += 4 + sz;
    }
    if (nested_oneway && !replied && is_waiting(tid) && sys3(199 /* getuid32 */, 0, 0, 0) >= 10000) {
        struct timespec_s ts = { 0, 30 * 1000 * 1000 };
        sys3(162 /* nanosleep */, (long)&ts, 0, 0);
    }
    if (replied) wait_slot(tid, 0);
    return (int)r;
}

/*
 * Host paths leak into the guest: /proc/self/fd/N links and the names of bound unix sockets are the phone's
 * real paths (".../files/images/<id>/root/dev/socket/zygote"). 7.0's zygote checks every open descriptor
 * against a whitelist of guest paths before forking and aborts ("Socket name not whitelisted", "Unable to
 * construct file descriptor table"). The image-root prefix is cut so the guest sees its own paths.
 */
static int strip_root(char *s, int n) {
    // find "/files/images/" then the following "/root", keep what comes after it
    for (int i = 0; i + 14 < n; i++) {
        const char *m = "/files/images/";
        int k = 0;
        while (k < 14 && s[i + k] == m[k]) k++;
        if (k < 14) continue;
        int j = i + 14;
        while (j < n && s[j] != '/') j++;
        if (j + 5 > n || s[j + 1] != 'r' || s[j + 2] != 'o' || s[j + 3] != 'o' || s[j + 4] != 't') return n;
        int from = j + 5, len = n - from;
        if (len <= 0) { s[0] = '/'; return 1; }
        for (int q = 0; q < len; q++) s[q] = s[from + q];
        return len;
    }
    return n;
}

#define SYS_readlink 85
#define SYS_readlinkat 332
#define SYS_getsockname 286
/* qemu's own ownership table stays open in every guest process; zygote (7.0+) only lets through descriptors on
 * whitelisted paths and reopens them by path after fork: it is shown as a framework jar that links to it */
static long guest_fd_name(char *buf, long n, unsigned long size) {
    n = strip_root(buf, (int)n);
    // stdio of guest processes: pipes to the host's log readers or the log file itself (on Android: /dev/null)
    int logfile = 0;
    for (int i = 0; i + 14 < n && !logfile; i++) {
        const char *m = "/files/images/";
        int k = 0;
        while (k < 14 && buf[i + k] == m[k]) k++;
        if (k == 14) for (int j = i + 14; j + 5 <= n; j++) if (buf[j] == '/' && buf[j + 1] == 'r' && buf[j + 2] == 'u' && buf[j + 3] == 'n' && buf[j + 4] == '/') { logfile = 1; break; }
    }
    // the property area stays open here (on Android it is only mapped): after fork nobody needs the descriptor
    { const char *pp = "/dev/__properties__"; int k = 0; while (k < n && pp[k] && buf[k] == pp[k]) k++; if (!pp[k]) logfile = 1; }
    if (size >= 9 && (logfile || (n > 5 && buf[0] == 'p' && buf[1] == 'i' && buf[2] == 'p' && buf[3] == 'e' && buf[4] == ':'))) {
        const char *dn = "/dev/null";
        for (int m = 0; m < 9; m++) buf[m] = dn[m];
        return 9;
    }
    const char *own = "/dhd.owners", *as = "/system/framework/aemu-owners.jar";
    int k = 0;
    while (k < n && own[k] && buf[k] == own[k]) k++;
    if (k == n && !own[k]) {
        int m = 0;
        while (as[m] && (unsigned long)m < size) { buf[m] = as[m]; m++; }
        n = m;
    }
    return n;
}
EXPORT long readlink(const char *path, char *buf, unsigned long size) {
    long r = sys3(SYS_readlink, (long)path, (long)buf, (long)size);
    if (r < 0) return fail(r);
    return guest_fd_name(buf, r, size);
}
EXPORT long readlinkat(int dirfd, const char *path, char *buf, unsigned long size) {
    long r = sys4(SYS_readlinkat, dirfd, (long)path, (long)buf, (long)size);
    if (r < 0) return fail(r);
    return guest_fd_name(buf, r, size);
}
struct sockaddr_un_ { unsigned short family; char path[108]; };
EXPORT int getsockname(int fd, void *addr, unsigned *len) {
    long r = sys3(SYS_getsockname, fd, (long)addr, (long)len);
    if (r < 0) return fail(r);
    struct sockaddr_un_ *u = (struct sockaddr_un_ *)addr;
    if (u && len && *len > 2 && u->family == 1 /* AF_UNIX */ && u->path[0]) {
        int n = (int)*len - 2;
        while (n > 0 && u->path[n - 1] == 0) n--;
        int m = strip_root(u->path, n);
        if (m != n) { u->path[m] = 0; *len = 2 + m + 1; }
    }
    return 0;
}
