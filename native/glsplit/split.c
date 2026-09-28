extern int __android_log_print(int, const char *, const char *, ...);
/*
 * libGLES_split — переходник перед GL-мостом (libGLES_bridge.so, гостевая часть моста автора).
 *
 * 1. Раздельные имена. Часть загрузчиков EGL (Samsung 4.3, AOSP 4.4+ в раздельном режиме) требуют
 *    libEGL_<тег>.so / libGLESv1_CM_<тег>.so / libGLESv2_<тег>.so. Этот файл кладётся под всеми
 *    именами, а вызовы уходят в единственный экземпляр моста — состояние EGL/GL общее.
 *
 * 2. Загрузка текстур. Мост передаёт на хост w*h*bpp байт, не учитывая GL_UNPACK_ALIGNMENT и
 *    GL_UNPACK_ROW_LENGTH, а драйвер телефона читает с выравниванием строк — при строках некратной
 *    длины (RGB, альфа-текстуры нечётной ширины) он лезет за конец буфера и роняет приложение.
 *    Здесь такие загрузки переупаковываются в плотный буфер с выравниванием 1.
 *
 * Зависит только от libc/libdl, одинаково грузится в bionic Android 2.3–6.0.
 */
#include "names.h"

typedef unsigned int GLenum;
typedef int GLint;
typedef int GLsizei;
typedef void GLvoid;

extern void *dlopen(const char *name, int flags);
extern void *dlsym(void *handle, const char *name);
extern void *malloc(unsigned int n);
extern void free(void *p);

#define N (sizeof(kNames) / sizeof(kNames[0]))
static void *g_lib;
static void *g_fn[N];

static void trap(void) { __builtin_trap(); }

static int es1_current(void);
static int zero_fn(void) { return 0; }
static volatile int g_any_es1;   /* в процессе есть контекст ES1 — только тогда проверяем поток */

__attribute__((visibility("hidden"))) void *aemu_split_resolve(unsigned idx) {
    /* функции ES2/ES3 в контексте ES1: у телефона-хозяина их нет в таблице ES1 — был бы вызов по нулю */
    if (g_any_es1 && kEs2Only[idx] && es1_current()) return (void *)zero_fn;
    void *f = g_fn[idx];
    if (f) return f;
    if (!g_lib) g_lib = dlopen("/system/lib/egl/libGLES_bridge.so", 0);
    if (g_lib) f = dlsym(g_lib, kNames[idx]);
    if (!f) f = (void *)trap;
    g_fn[idx] = f;
    return f;
}

#define GL_UNPACK_ALIGNMENT 0x0CF5
#define GL_UNPACK_ROW_LENGTH 0x0CF2
#define GL_UNPACK_SKIP_ROWS 0x0CF3
#define GL_UNPACK_SKIP_PIXELS 0x0CF4
#define GL_PIXEL_UNPACK_BUFFER 0x88EC

/* состояние распаковки текущего потока (гость обычно рисует из одного потока на контекст) */
static int t_align = 4, t_rowlen, t_skiprows, t_skippix, t_pbo;

typedef void (*PixelStoreFn)(GLenum, GLint);
typedef void (*BindBufferFn)(GLenum, unsigned);
typedef void (*TexImageFn)(GLenum, GLint, GLint, GLsizei, GLsizei, GLint, GLenum, GLenum, const GLvoid *);
typedef void (*TexSubImageFn)(GLenum, GLint, GLint, GLint, GLsizei, GLsizei, GLenum, GLenum, const GLvoid *);

__attribute__((visibility("default"))) void glPixelStorei(GLenum pname, GLint param) {
    switch (pname) {
        case GL_UNPACK_ALIGNMENT: t_align = param; break;
        case GL_UNPACK_ROW_LENGTH: t_rowlen = param; break;
        case GL_UNPACK_SKIP_ROWS: t_skiprows = param; break;
        case GL_UNPACK_SKIP_PIXELS: t_skippix = param; break;
    }
    ((PixelStoreFn)aemu_split_resolve(IDX_glPixelStorei))(pname, param);
}

static void va_bind_note(GLenum target, unsigned buffer);
static void img_forget_bound(void);

__attribute__((visibility("default"))) void glBindBuffer(GLenum target, unsigned buffer) {
    if (target == GL_PIXEL_UNPACK_BUFFER) t_pbo = buffer != 0;
    va_bind_note(target, buffer);
    ((BindBufferFn)aemu_split_resolve(IDX_glBindBuffer))(target, buffer);
}

/* байт на пиксель; 0 — формат, который не переупаковываем (сжатый, неизвестный) */
static int bytes_per_pixel(GLenum format, GLenum type) {
    int comps;
    switch (format) {
        case 0x1906: /* ALPHA */ case 0x1909: /* LUMINANCE */ case 0x1903: /* RED */
        case 0x1902: /* DEPTH_COMPONENT */ case 0x8D94: /* RED_INTEGER */ comps = 1; break;
        case 0x190A: /* LUMINANCE_ALPHA */ case 0x8227: /* RG */ case 0x8228: /* RG_INTEGER */ comps = 2; break;
        case 0x1907: /* RGB */ case 0x8D98: /* RGB_INTEGER */ comps = 3; break;
        case 0x1908: /* RGBA */ case 0x80E1: /* BGRA */ case 0x8D99: /* RGBA_INTEGER */ comps = 4; break;
        default: return 0;
    }
    switch (type) {
        case 0x1401: /* UNSIGNED_BYTE */ case 0x1400: /* BYTE */ return comps;
        case 0x1403: /* UNSIGNED_SHORT */ case 0x1402: /* SHORT */
        case 0x140B: /* HALF_FLOAT */ case 0x8D61: /* HALF_FLOAT_OES */ return comps * 2;
        case 0x1405: /* UNSIGNED_INT */ case 0x1404: /* INT */ case 0x1406: /* FLOAT */ return comps * 4;
        case 0x8363: /* 5_6_5 */ case 0x8033: /* 4_4_4_4 */ case 0x8034: /* 5_5_5_1 */ return 2;
        case 0x8368: /* 2_10_10_10_REV */ case 0x84FA: /* 24_8 */ return 4;
        default: return 0;
    }
}

/* без memcpy: в bionic 2.x нет __aeabi_memcpy, а зависеть от libgcc не хотим */
static void copy_bytes(unsigned char *d, const unsigned char *s, unsigned long n) {
    if ((((unsigned long)d | (unsigned long)s | n) & 3) == 0) {
        unsigned int *dw = (unsigned int *)d; const unsigned int *sw = (const unsigned int *)s;
        for (unsigned long i = 0; i < n / 4; i++) dw[i] = sw[i];
        return;
    }
    for (unsigned long i = 0; i < n; i++) d[i] = s[i];
}

/*
 * Если данные гостя лежат не плотно (выравнивание строк, длина строки, пропуски) — возвращает
 * плотную копию (освободить free) и выставляет *need = 1; иначе возвращает исходный указатель.
 */
static const void *tighten(GLsizei w, GLsizei h, GLenum format, GLenum type, const void *pixels, int *need) {
    *need = 0;
    if (!pixels || t_pbo || w <= 0 || h <= 0) return pixels;
    int bpp = bytes_per_pixel(format, type);
    if (!bpp) return pixels;
    int align = (t_align == 1 || t_align == 2 || t_align == 4 || t_align == 8) ? t_align : 4;
    unsigned long row = (unsigned long)w * bpp;
    unsigned long srcRowPixels = t_rowlen > 0 ? (unsigned long)t_rowlen : (unsigned long)w;
    unsigned long stride = (srcRowPixels * bpp + align - 1) & ~(unsigned long)(align - 1); /* align — степень двойки */
    if (stride == row && t_skiprows == 0 && t_skippix == 0) {
        /* плотно, но драйвер хоста всё равно выровняет последнюю строку — это безопасно,
           только если выравнивание 1 или строка кратна ему */
        if (align == 1 || (row & (unsigned long)(align - 1)) == 0) return pixels;
    }
    const unsigned char *src = (const unsigned char *)pixels + (unsigned long)t_skiprows * stride + (unsigned long)t_skippix * bpp;
    unsigned char *dst = (unsigned char *)malloc(row * h);
    if (!dst) return pixels;
    for (GLsizei y = 0; y < h; y++) copy_bytes(dst + (unsigned long)y * row, src + (unsigned long)y * stride, row);
    *need = 1;
    return dst;
}

static void unpack_tight(int on) {
    PixelStoreFn ps = (PixelStoreFn)aemu_split_resolve(IDX_glPixelStorei);
    if (on) {
        ps(GL_UNPACK_ALIGNMENT, 1);
        if (t_rowlen) ps(GL_UNPACK_ROW_LENGTH, 0);
        if (t_skiprows) ps(GL_UNPACK_SKIP_ROWS, 0);
        if (t_skippix) ps(GL_UNPACK_SKIP_PIXELS, 0);
    } else {
        ps(GL_UNPACK_ALIGNMENT, t_align);
        if (t_rowlen) ps(GL_UNPACK_ROW_LENGTH, t_rowlen);
        if (t_skiprows) ps(GL_UNPACK_SKIP_ROWS, t_skiprows);
        if (t_skippix) ps(GL_UNPACK_SKIP_PIXELS, t_skippix);
    }
}

__attribute__((visibility("default"))) void glTexImage2D(GLenum target, GLint level, GLint ifmt, GLsizei w, GLsizei h,
                                                          GLint border, GLenum format, GLenum type, const GLvoid *pixels) {
    TexImageFn f = (TexImageFn)aemu_split_resolve(IDX_glTexImage2D);
    img_forget_bound();   /* the texture now has its own storage, no longer an EGLImage alias */
    int need;
    const void *p = tighten(w, h, format, type, pixels, &need);
    if (need) unpack_tight(1);
    f(target, level, ifmt, w, h, border, format, type, p);
    if (need) { unpack_tight(0); free((void *)p); }
}

__attribute__((visibility("default"))) void glTexSubImage2D(GLenum target, GLint level, GLint x, GLint y, GLsizei w, GLsizei h,
                                                             GLenum format, GLenum type, const GLvoid *pixels) {
    TexSubImageFn f = (TexSubImageFn)aemu_split_resolve(IDX_glTexSubImage2D);
    int need;
    const void *p = tighten(w, h, format, type, pixels, &need);
    if (need) unpack_tight(1);
    f(target, level, x, y, w, h, format, type, p);
    if (need) { unpack_tight(0); free((void *)p); }
}

/*
 * Контекст ES2 для SurfaceFlinger 4.4. Его RenderEngine создаёт контекст без EGL_CONTEXT_CLIENT_VERSION
 * (то есть ES1) и рисует фиксированным конвейером, а сборки MIUI/MTK при этом ещё и зовут шейдерные
 * функции ES2 (размытие, DRM-значок). На настоящем железе такой вызов в контексте ES1 — ошибка в журнале,
 * а драйвер телефона-хозяина роняет мост нулевым указателем. Если движок выставил AEMU_GL_ES2=1
 * (только для surfaceflinger), просим ES2 — тогда SurfaceFlinger сам выбирает GLES20RenderEngine.
 */
extern char *getenv(const char *name);
typedef void *(*CreateContextFn)(void *dpy, void *config, void *share, const GLint *attribs);
#define EGL_CONTEXT_CLIENT_VERSION 0x3098
#define EGL_NONE 0x3038

static void ctx_remember(void *ctx, int ver);
extern long syscall(long n, ...);

static int is_surfaceflinger(void) {
    static int v = -1;
    if (v < 0) {
        char buf[128];
        long fd = syscall(5 /* open */, "/proc/self/cmdline", 0, 0);
        long n = fd >= 0 ? syscall(3 /* read */, fd, buf, sizeof(buf) - 1) : 0;
        if (fd >= 0) syscall(6 /* close */, fd);
        v = 0;
        if (n > 0) {
            buf[n] = 0;
            const char *want = "surfaceflinger";
            for (long i = 0; i + 14 <= n && !v; i++) {
                int k = 0;
                while (k < 14 && buf[i + k] == want[k]) k++;
                if (k == 14) v = 1;
            }
        }
    }
    return v;
}

__attribute__((visibility("default"))) void *eglCreateContext(void *dpy, void *config, void *share, const GLint *attribs) {
    CreateContextFn f = (CreateContextFn)aemu_split_resolve(IDX_eglCreateContext);
    static int force = -1;
    if (force < 0) { const char *v = getenv("AEMU_GL_ES2"); force = v && v[0] == '1'; }
    int ver = 1;
    if (attribs) for (int i = 0; attribs[i] != EGL_NONE && i < 64; i += 2)
        if (attribs[i] == EGL_CONTEXT_CLIENT_VERSION) ver = attribs[i + 1];
    void *ctx;
    if (force && ver < 2) {
        GLint list[64];
        int n = 0;
        if (attribs) for (int i = 0; attribs[i] != EGL_NONE && n < 60; i += 2) {
            if (attribs[i] == EGL_CONTEXT_CLIENT_VERSION) continue;
            list[n++] = attribs[i]; list[n++] = attribs[i + 1];
        }
        list[n++] = EGL_CONTEXT_CLIENT_VERSION; list[n++] = 2;
        list[n] = EGL_NONE;
        ver = 2;
        ctx = f(dpy, config, share, list);
    } else ctx = f(dpy, config, share, attribs);
    /* гостевая часть моста всем контекстам SurfaceFlinger даёт ES1 (и сообщает «ES-CM 1.1»): учитываем это */
    if (is_surfaceflinger()) ver = 1;
    ctx_remember(ctx, ver);
    return ctx;
}

/* ------------------------------------------------------------ версии контекстов по потокам */

extern long syscall(long n, ...);
#define MAXCTX 256
#define MAXTHR 256
static void *g_ctx[MAXCTX];
static unsigned char g_ctxver[MAXCTX];
static int g_tid[MAXTHR];
static unsigned char g_thrver[MAXTHR];

static void ctx_remember(void *ctx, int ver) {
    if (!ctx) return;
    if (ver < 2) g_any_es1 = 1;
    for (int i = 0; i < MAXCTX; i++) if (g_ctx[i] == ctx || g_ctx[i] == 0) { g_ctx[i] = ctx; g_ctxver[i] = (unsigned char)ver; return; }
}
static int ctx_version(void *ctx) {
    for (int i = 0; i < MAXCTX; i++) if (g_ctx[i] == ctx) return g_ctxver[i];
    return 2;
}
static int es1_current(void) {
    int tid = (int)syscall(224 /* gettid */);
    for (int i = 0; i < MAXTHR; i++) if (g_tid[i] == tid) return g_thrver[i] < 2;
    return 0;
}
static void thread_set(int ver) {
    int tid = (int)syscall(224);
    int free = -1;
    for (int i = 0; i < MAXTHR; i++) {
        if (g_tid[i] == tid) { g_thrver[i] = (unsigned char)ver; return; }
        if (g_tid[i] == 0 && free < 0) free = i;
    }
    if (free >= 0) { g_thrver[free] = (unsigned char)ver; g_tid[free] = tid; }
}

typedef unsigned (*MakeCurrentFn)(void *dpy, void *draw, void *read, void *ctx);
__attribute__((visibility("default"))) unsigned eglMakeCurrent(void *dpy, void *draw, void *read, void *ctx) {
    unsigned r = ((MakeCurrentFn)aemu_split_resolve(IDX_eglMakeCurrent))(dpy, draw, read, ctx);
    if (r) thread_set(ctx ? ctx_version(ctx) : 2);
    return r;
}

/* ------------------------------------------------------------ client-side vertex arrays (ES2)
 *
 * The bridge forwards glVertexAttribPointer with no bound GL_ARRAY_BUFFER as a plain offset: the guest
 * pointer never reaches the host, nothing gets drawn (GB live wallpapers, games → black/flickering
 * frames). Before each draw we copy the enabled client arrays into a scratch VBO, point the attributes
 * at it, draw, then restore the app's state. ES1 contexts are left alone: the bridge handles them.
 */
#define GL_ARRAY_BUFFER 0x8892
#define GL_ELEMENT_ARRAY_BUFFER 0x8893
#define GL_STREAM_DRAW 0x88E0
#define MAXATTR 16
struct va_attr { const void *ptr; GLint size; GLenum type; unsigned char norm, enabled, client; GLsizei stride; };
struct va_state { int tid; unsigned array_buf, elem_buf; struct va_attr a[MAXATTR]; };
static struct va_state g_va[64];

static struct va_state *va_cur(void) {
    int tid = (int)syscall(224 /* gettid */);
    int free = -1;
    for (int i = 0; i < 64; i++) {
        if (g_va[i].tid == tid) return &g_va[i];
        if (g_va[i].tid == 0 && free < 0) free = i;
    }
    if (free < 0) free = tid & 63;   /* table full: reuse a slot (rare, threads are few) */
    struct va_state *v = &g_va[free];
    for (unsigned k = 0; k < sizeof(*v); k++) ((unsigned char *)v)[k] = 0;
    v->tid = tid;
    return v;
}

static void va_bind_note(GLenum target, unsigned buffer) {
    if (target == GL_ARRAY_BUFFER) va_cur()->array_buf = buffer;
    else if (target == GL_ELEMENT_ARRAY_BUFFER) va_cur()->elem_buf = buffer;
}

typedef void (*AttribPtrFn)(unsigned, GLint, GLenum, unsigned char, GLsizei, const void *);
typedef void (*AttribArrFn)(unsigned);
typedef void (*DrawArraysFn)(GLenum, GLint, GLsizei);
typedef void (*DrawElementsFn)(GLenum, GLsizei, GLenum, const void *);
typedef void (*GenBuffersFn)(GLsizei, unsigned *);
typedef void (*BufferDataFn)(GLenum, long, const void *, GLenum);
typedef void (*BufferSubDataFn)(GLenum, long, long, const void *);

__attribute__((visibility("default"))) void glVertexAttribPointer(unsigned idx, GLint size, GLenum type, unsigned char norm,
                                                                   GLsizei stride, const void *ptr) {
    struct va_state *v = va_cur();
    if (idx < MAXATTR) {
        struct va_attr *a = &v->a[idx];
        a->ptr = ptr; a->size = size; a->type = type; a->norm = norm; a->stride = stride;
        a->client = v->array_buf == 0 && ptr != 0;
    }
    ((AttribPtrFn)aemu_split_resolve(IDX_glVertexAttribPointer))(idx, size, type, norm, stride, ptr);
}
__attribute__((visibility("default"))) void glEnableVertexAttribArray(unsigned idx) {
    if (idx < MAXATTR) va_cur()->a[idx].enabled = 1;
    ((AttribArrFn)aemu_split_resolve(IDX_glEnableVertexAttribArray))(idx);
}
__attribute__((visibility("default"))) void glDisableVertexAttribArray(unsigned idx) {
    if (idx < MAXATTR) va_cur()->a[idx].enabled = 0;
    ((AttribArrFn)aemu_split_resolve(IDX_glDisableVertexAttribArray))(idx);
}

static int type_size(GLenum t) {
    switch (t) {
        case 0x1400: case 0x1401: return 1;              /* BYTE, UNSIGNED_BYTE */
        case 0x1402: case 0x1403: case 0x140B: case 0x8D61: return 2; /* SHORT, USHORT, HALF_FLOAT(_OES) */
        default: return 4;                               /* FLOAT, FIXED, INT… */
    }
}

/* uploads client arrays for vertices [0, nverts); returns the scratch buffer name or 0 if nothing to do */
static unsigned va_upload(struct va_state *v, long nverts) {
    static int on = -1;
    if (on < 0) { const char *e = getenv("AEMU_GL_CLIENT_ARRAYS"); on = !(e && e[0] == '0'); }
    if (!on || nverts <= 0 || es1_current()) return 0;
    long total = 0, off[MAXATTR];
    for (int i = 0; i < MAXATTR; i++) {
        struct va_attr *a = &v->a[i];
        off[i] = -1;
        if (!a->enabled || !a->client) continue;
        long esz = (long)a->size * type_size(a->type);
        long stride = a->stride ? a->stride : esz;
        off[i] = total;
        total += ((stride * (nverts - 1) + esz) + 3) & ~3L;
    }
    if (!total) return 0;
    /* one glBufferData with the data: the bridge drops glBufferSubData payloads on large buffers */
    unsigned char *blob = (unsigned char *)malloc((unsigned)total);
    if (!blob) return 0;
    for (int i = 0; i < MAXATTR; i++) {
        if (off[i] < 0) continue;
        struct va_attr *a = &v->a[i];
        long esz = (long)a->size * type_size(a->type);
        long stride = a->stride ? a->stride : esz;
        copy_bytes(blob + off[i], (const unsigned char *)a->ptr, (unsigned long)(stride * (nverts - 1) + esz));
    }
    unsigned buf = 0;
    ((GenBuffersFn)aemu_split_resolve(IDX_glGenBuffers))(1, &buf);
    if (!buf) { free(blob); return 0; }
    BindBufferFn bind = (BindBufferFn)aemu_split_resolve(IDX_glBindBuffer);
    AttribPtrFn ap = (AttribPtrFn)aemu_split_resolve(IDX_glVertexAttribPointer);
    bind(GL_ARRAY_BUFFER, buf);
    ((BufferDataFn)aemu_split_resolve(IDX_glBufferData))(GL_ARRAY_BUFFER, total, blob, GL_STREAM_DRAW);
    free(blob);
    for (int i = 0; i < MAXATTR; i++) {
        if (off[i] < 0) continue;
        struct va_attr *a = &v->a[i];
        ap((unsigned)i, a->size, a->type, a->norm, a->stride, (const void *)off[i]);
    }
    return buf;
}

static void va_restore(struct va_state *v, unsigned buf) {
    BindBufferFn bind = (BindBufferFn)aemu_split_resolve(IDX_glBindBuffer);
    AttribPtrFn ap = (AttribPtrFn)aemu_split_resolve(IDX_glVertexAttribPointer);
    bind(GL_ARRAY_BUFFER, 0);
    for (int i = 0; i < MAXATTR; i++) {
        struct va_attr *a = &v->a[i];
        if (a->enabled && a->client) ap((unsigned)i, a->size, a->type, a->norm, a->stride, a->ptr);
    }
    bind(GL_ARRAY_BUFFER, v->array_buf);
    ((GenBuffersFn)aemu_split_resolve(IDX_glDeleteBuffers))(1, &buf);  /* same signature as glGenBuffers */
}

__attribute__((visibility("default"))) void glDrawArrays(GLenum mode, GLint first, GLsizei count) {
    struct va_state *v = va_cur();
    unsigned buf = count > 0 ? va_upload(v, (long)first + count) : 0;
    ((DrawArraysFn)aemu_split_resolve(IDX_glDrawArrays))(mode, first, count);
    if (buf) va_restore(v, buf);
}

__attribute__((visibility("default"))) void glDrawElements(GLenum mode, GLsizei count, GLenum type, const void *indices) {
    struct va_state *v = va_cur();
    unsigned buf = 0;
    /* vertex count is only known when the indices are in client memory too */
    if (count > 0 && v->elem_buf == 0 && indices) {
        long maxi = 0;
        for (GLsizei i = 0; i < count; i++) {
            long x = type == 0x1401 ? ((const unsigned char *)indices)[i]
                   : type == 0x1403 ? ((const unsigned short *)indices)[i]
                   : (long)((const unsigned int *)indices)[i];
            if (x > maxi) maxi = x;
        }
        buf = va_upload(v, maxi + 1);
    }
    ((DrawElementsFn)aemu_split_resolve(IDX_glDrawElements))(mode, count, type, indices);
    if (buf) va_restore(v, buf);
}

/* ------------------------------------------------------------ EGLImage textures (SurfaceFlinger layers)
 *
 * On real hardware an EGLImage texture aliases the gralloc buffer, so every new frame the app queues is
 * visible as soon as SurfaceFlinger binds the texture. The bridge instead copies the buffer's pixels once,
 * inside glEGLImageTargetTexture2DOES. GB/ICS SurfaceFlinger calls that only the first time it sees a
 * buffer and later just binds the texture — the layer freezes on its first contents (still animations)
 * or on an empty buffer (black flicker with double buffering). Re-target the image on bind, at most once
 * per SurfaceFlinger frame, so the bridge re-reads the buffer.
 */
#define GL_TEXTURE_2D 0x0DE1
#define GL_TEXTURE_EXTERNAL_OES 0x8D65
#define MAXIMG 256
struct img_tex { unsigned tex; GLenum target; void *image; unsigned frame; };
static struct img_tex g_img[MAXIMG];
static unsigned g_frame = 1, g_bound2d, g_boundext;

typedef void (*BindTexFn)(GLenum, unsigned);
typedef void (*ImageTargetFn)(GLenum, void *);
typedef void (*DeleteTexFn)(GLsizei, const unsigned *);
typedef unsigned (*SwapFn)(void *, void *);

static struct img_tex *img_find(unsigned tex) {
    if (!tex) return 0;
    for (int i = 0; i < MAXIMG; i++) if (g_img[i].tex == tex) return &g_img[i];
    return 0;
}
static void img_forget_bound(void) {
    struct img_tex *e = img_find(g_bound2d);
    if (e && e->target == GL_TEXTURE_2D) e->tex = 0;
}

__attribute__((visibility("default"))) void glBindTexture(GLenum target, unsigned tex) {
    ((BindTexFn)aemu_split_resolve(IDX_glBindTexture))(target, tex);
    if (target == GL_TEXTURE_2D) g_bound2d = tex;
    else if (target == GL_TEXTURE_EXTERNAL_OES) g_boundext = tex;
    else return;
    static int on = -1;
    if (on < 0) { const char *v = getenv("AEMU_GL_RETARGET"); on = !(v && v[0] == '0'); }
    struct img_tex *e = on ? img_find(tex) : 0;
    if (e && e->target == target && e->frame != g_frame) {
        e->frame = g_frame;
        ((ImageTargetFn)aemu_split_resolve(IDX_glEGLImageTargetTexture2DOES))(target, e->image);
    }
}

__attribute__((visibility("default"))) void glEGLImageTargetTexture2DOES(GLenum target, void *image) {
    ((ImageTargetFn)aemu_split_resolve(IDX_glEGLImageTargetTexture2DOES))(target, image);
    unsigned tex = target == GL_TEXTURE_EXTERNAL_OES ? g_boundext : g_bound2d;
    if (!tex) return;
    struct img_tex *e = img_find(tex);
    if (!e) for (int i = 0; i < MAXIMG && !e; i++) if (!g_img[i].tex) e = &g_img[i];
    if (!e) e = &g_img[tex % MAXIMG];
    e->tex = tex; e->target = target; e->image = image; e->frame = g_frame;
}

__attribute__((visibility("default"))) void glDeleteTextures(GLsizei n, const unsigned *tex) {
    for (GLsizei i = 0; tex && i < n; i++) { struct img_tex *e = img_find(tex[i]); if (e) e->tex = 0; }
    ((DeleteTexFn)aemu_split_resolve(IDX_glDeleteTextures))(n, tex);
}

__attribute__((visibility("default"))) unsigned eglSwapBuffers(void *dpy, void *surface) {
    g_frame++;
    return ((SwapFn)aemu_split_resolve(IDX_eglSwapBuffers))(dpy, surface);
}

/*
 * 7.0+ libEGL takes eglSwapBuffersWithDamageKHR / eglSetDamageRegionKHR from the driver when it offers them, and
 * hwui then presents only through them. The bridge hands out empty stubs for both, so no app frame ever reached
 * SurfaceFlinger (black screen): damage is ignored and the whole surface is swapped.
 */
static unsigned swap_with_damage(void *dpy, void *surface, const GLint *rects, GLint n) {
    (void)rects; (void)n;
    return eglSwapBuffers(dpy, surface);
}
static unsigned set_damage_region(void *dpy, void *surface, const GLint *rects, GLint n) {
    (void)dpy; (void)surface; (void)rects; (void)n;
    return 1;
}
/*
 * 7.0+ app window buffers carry SW_READ/WRITE_OFTEN usage (0x933); the bridge refuses EGLImages for them, so
 * SurfaceFlinger never latched launcher or status bar frames and the apps stalled on dequeue. On failure the
 * image is retried with the software bits hidden (ANativeWindowBuffer.usage, after the 32-byte base header).
 */
/* 7.x SurfaceFlinger runs with stdin closed, so a gralloc buffer can land on descriptor 0, which the bridge
 * takes for "no fd" (layer shown empty, the app stalls): keep 0..2 occupied */
__attribute__((constructor)) static void hold_std_fds(void) {
    for (int fd = 0; fd < 3; fd++)
        if (syscall(55 /* fcntl */, fd, 1 /* F_GETFD */) < 0) syscall(5 /* open */, "/dev/null", 2 /* O_RDWR */, 0);
}

typedef void *(*CreateImageFn)(void *dpy, void *ctx, unsigned target, void *buf, const GLint *attribs);
__attribute__((visibility("default"))) void *eglCreateImageKHR(void *dpy, void *ctx, unsigned target, void *buf, const GLint *attribs) {
    CreateImageFn f = (CreateImageFn)aemu_split_resolve(IDX_eglCreateImageKHR);
    void *img = f(dpy, ctx, target, buf, attribs);
    if (img || target != 0x3140 /* EGL_NATIVE_BUFFER_ANDROID */ || !buf) return img;
    int *usage = (int *)((char *)buf + 48);
    int old = *usage;
    if (!(old & 0xff)) return img;
    *usage = old & ~0xff;
    img = f(dpy, ctx, target, buf, attribs);
    *usage = old;
    return img;
}

typedef void *(*ProcFn)(const char *);
__attribute__((visibility("default"))) void *eglGetProcAddress(const char *name) {
    static const char *const names[] = { "eglSwapBuffersWithDamageKHR", "eglSwapBuffersWithDamageEXT", "eglSetDamageRegionKHR" };
    static void *const fns[] = { (void *)swap_with_damage, (void *)swap_with_damage, (void *)set_damage_region };
    for (int i = 0; name && i < 3; i++) {
        int k = 0;
        while (names[i][k] && name[k] == names[i][k]) k++;
        if (!names[i][k] && !name[k]) return fns[i];
    }
    return ((ProcFn)aemu_split_resolve(IDX_eglGetProcAddress))(name);
}

/* ------------------------------------------------------------ extra SurfaceFlinger windows (4.3+)
 *
 * The bridge treats every window surface SurfaceFlinger creates as "the screen" and moves the phone's
 * display onto it. 4.3 SurfaceFlinger also creates window surfaces for virtual displays / screenshots
 * (pressing Home in TouchWiz does it): the bridge switches to that 1x1 window, fails to attach it and never
 * switches back — the emulator shows an empty screen. In surfaceflinger only the first window (the primary
 * display) gets a real window surface; any other window gets an off-screen pbuffer of the same size.
 */
typedef void *(*CreateWinFn)(void *dpy, void *config, void *win, const GLint *attribs);
typedef void *(*CreatePbufFn)(void *dpy, void *config, const GLint *attribs);
typedef int (*WinQueryFn)(const void *win, int what, int *value);
static void *g_primary_win;
typedef unsigned (*GetAttrFn)(void *dpy, void *config, int attr, int *value);
typedef unsigned (*ChooseFn)(void *dpy, const GLint *attribs, void **configs, int size, int *num);

__attribute__((visibility("default"))) void *eglCreateWindowSurface(void *dpy, void *config, void *win, const GLint *attribs) {
    CreateWinFn f = (CreateWinFn)aemu_split_resolve(IDX_eglCreateWindowSurface);
    if (!is_surfaceflinger() || !win) {
        void *r = f(dpy, config, win, attribs);
        if (r || !win) return r;
        /* The window's pixel format does not match the config (RenderScript: RGBA_8888 window, RGB565
         * config — GB's 3D all-apps list stayed blank): retry with a config of the window's format. */
        int fmt = 0, depth = 0, stencil = 0, n = 0;
        WinQueryFn q = *(WinQueryFn *)((char *)win + 84);
        if (q) q(win, 2 /* NATIVE_WINDOW_FORMAT */, &fmt);
        GetAttrFn ga = (GetAttrFn)aemu_split_resolve(IDX_eglGetConfigAttrib);
        ga(dpy, config, 0x3025 /* EGL_DEPTH_SIZE */, &depth);
        ga(dpy, config, 0x3026 /* EGL_STENCIL_SIZE */, &stencil);
        int rgb565 = fmt == 4, alpha = fmt == 1 ? 8 : 0;
        GLint ca[] = { 0x3024, rgb565 ? 5 : 8, 0x3023, rgb565 ? 6 : 8, 0x3022, rgb565 ? 5 : 8, 0x3021, alpha,
                       0x3025, depth, 0x3026, stencil, 0x3033 /* SURFACE_TYPE */, 4 /* WINDOW */,
                       0x3040 /* RENDERABLE_TYPE */, 4 /* ES2 */, EGL_NONE };
        void *cfg = 0;
        if (((ChooseFn)aemu_split_resolve(IDX_eglChooseConfig))(dpy, ca, &cfg, 1, &n) && n > 0 && cfg != config)
            r = f(dpy, cfg, win, attribs);
        return r;
    }
    if (!g_primary_win) g_primary_win = win;
    if (win == g_primary_win) return f(dpy, config, win, attribs);
    int w = 1, h = 1;
    WinQueryFn q = *(WinQueryFn *)((char *)win + 84);   /* ANativeWindow::query (32-bit layout) */
    if (q) { q(win, 0 /* NATIVE_WINDOW_WIDTH */, &w); q(win, 1 /* NATIVE_WINDOW_HEIGHT */, &h); }
    if (w <= 0 || w > 4096) w = 1;
    if (h <= 0 || h > 4096) h = 1;
    GLint pa[] = { 0x3057 /* EGL_WIDTH */, w, 0x3056 /* EGL_HEIGHT */, h, EGL_NONE };
    return ((CreatePbufFn)aemu_split_resolve(IDX_eglCreatePbufferSurface))(dpy, config, pa);
}
