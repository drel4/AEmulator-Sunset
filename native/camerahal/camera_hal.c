/* AEmulator-owned HAL1 camera. Standard device ABI, host NV21/JPEG transport;
 * no vendor binaries, private sensor drivers, recording or raw capture. */
#include "abi.h"
#include "transport.h"
#include <stdlib.h>
#include <stdio.h>
#include <pthread.h>
#include <dlfcn.h>
#define EXPORT __attribute__((visibility("default")))
#define MSG_ERROR 1
#define MSG_SHUTTER 2
#define MSG_FOCUS 4
#define MSG_PREVIEW 0x10
#define MSG_JPEG 0x100
#define MSG_RAW_NOTIFY 0x200

typedef struct {
    cam_device device;
    pthread_mutex_t mutex;
    pthread_mutex_t control;
    pthread_t thread;
    pthread_t execution;
    int in_worker;
    int id, fd, started, running, messages, capture, quality, rotation;
    cam_notify notify; cam_data data; cam_allocate allocate; void *user;
    cam_window *window;
} camera_state;
EXPORT cam_module HMI;
static pthread_mutex_t metadata_mutex = PTHREAD_MUTEX_INITIALIZER;
static int camera_count = -1, camera_info[2][2];
static const cam_gralloc *gralloc;
static pthread_mutex_t gralloc_mutex = PTHREAD_MUTEX_INITIALIZER;
static int count_cameras(void) {
    pthread_mutex_lock(&metadata_mutex);
    if (camera_count < 0) {
        int fd = cam_connect(0, 0, 0);
        if (fd >= 0) {
            unsigned char packet[24];
            if (!cam_io(fd, packet, 8, 0, 1000) && !memcmp(packet, "INF1", 4)) {
                unsigned n = cam_le32(packet + 4);
                if (n <= 2 && !cam_io(fd, packet + 8, n * 8, 0, 1000)) {
                    int valid = 1;
                    for (unsigned i = 0; i < n; i++) {
                        camera_info[i][0] = cam_le32(packet + 8 + i * 8);
                        camera_info[i][1] = cam_le32(packet + 12 + i * 8);
                        if (camera_info[i][0] < 0 || camera_info[i][0] > 1 || camera_info[i][1] < 0 ||
                            camera_info[i][1] > 270 || camera_info[i][1] % 90) valid = 0;
                    }
                    if (valid) camera_count = n;
                }
            }
            close(fd);
        }
    }
    int n = camera_count < 0 ? 0 : camera_count;
    pthread_mutex_unlock(&metadata_mutex);
    return n;
}
static int get_info(int id, void *out) {
    if (!out || id < 0 || id >= count_cameras()) return -EINVAL;
    // HAL module version 1.0 only owns the original two fields (ICS allocates 8 bytes).
    int *p = out; p[0] = camera_info[id][0]; p[1] = camera_info[id][1];
    return 0;
}
static camera_state *state(cam_device *d) { return (camera_state *)d->priv; }
static int running(camera_state *s) { return __atomic_load_n(&s->running, __ATOMIC_ACQUIRE); }
static int worker_is_self(camera_state *s) {
    pthread_mutex_lock(&s->mutex);
    int result = s->in_worker && pthread_equal(s->execution, pthread_self());
    pthread_mutex_unlock(&s->mutex);
    return result;
}
static int load_gralloc(void) {
    pthread_mutex_lock(&gralloc_mutex);
    if (!gralloc) {
        void *lib = dlopen("libhardware.so", RTLD_NOW | RTLD_LOCAL);
        if (lib) {
            int (*get_module)(const char *, const cam_module_common **) = dlsym(lib, "hw_get_module");
            const cam_module_common *module = 0;
            if (get_module && !get_module("gralloc", &module) && module) gralloc = (const cam_gralloc *)module;
            if (!gralloc) dlclose(lib); // keep successful loader reference for the lifetime of the HAL
        }
    }
    int ok = gralloc && gralloc->lock && gralloc->unlock;
    pthread_mutex_unlock(&gralloc_mutex);
    return ok;
}
static int set_window(cam_device *d, cam_window *w) {
    if (w && (!w->geometry || !w->dequeue || !w->enqueue || !w->cancel || !load_gralloc())) return -ENOSYS;
    camera_state *s = state(d);
    pthread_mutex_lock(&s->mutex);
    int r = 0;
    if (w) {
        int minimum = 0;
        if (w->min_buffers) r = w->min_buffers(w, &minimum);
        if (!r && (minimum < 0 || minimum > 8)) r = -EINVAL;
        if (!r && w->usage) r = w->usage(w, 0x30); // CPU write often
        if (!r && w->buffer_count) r = w->buffer_count(w, minimum + 2);
        if (!r) r = w->geometry(w, CAM_WIDTH, CAM_HEIGHT, 1 /* RGBA8888 */);
    }
    if (!r) s->window = w;
    pthread_mutex_unlock(&s->mutex);
    return r;
}
static unsigned char clamp_color(int value) { return value < 0 ? 0 : value > 255 ? 255 : value; }
static int render(camera_state *s, const unsigned char *nv21) {
    int r = 0;
    pthread_mutex_lock(&s->mutex);
    cam_window *w = s->window;
    if (w) {
        const void **buffer = 0; int stride = 0; void *pixels = 0;
        r = w->dequeue(w, &buffer, &stride);
        if (!r) {
            if (!buffer || !*buffer || stride < CAM_WIDTH || stride > 16384) r = -EINVAL;
            if (!r && w->lock_buffer) r = w->lock_buffer(w, buffer);
            if (!r) r = gralloc->lock(gralloc, *buffer, 0x30, 0, 0, CAM_WIDTH, CAM_HEIGHT, &pixels);
            if (!r && pixels) {
                for (unsigned y = 0; y < CAM_HEIGHT; y++) for (unsigned x = 0; x < CAM_WIDTH; x++) {
                    int yy = nv21[y * CAM_WIDTH + x] - 16;
                    if (yy < 0) yy = 0;
                    unsigned uv = CAM_WIDTH * CAM_HEIGHT + (y / 2) * CAM_WIDTH + (x & ~1u);
                    int v = nv21[uv] - 128, u = nv21[uv + 1] - 128;
                    unsigned char *p = (unsigned char *)pixels + (y * stride + x) * 4;
                    p[0] = clamp_color((298 * yy + 409 * v + 128) >> 8);
                    p[1] = clamp_color((298 * yy - 100 * u - 208 * v + 128) >> 8);
                    p[2] = clamp_color((298 * yy + 516 * u + 128) >> 8); p[3] = 255;
                }
                r = gralloc->unlock(gralloc, *buffer);
            } else if (!r) { gralloc->unlock(gralloc, *buffer); r = -EFAULT; }
            if (!r) { r = w->enqueue(w, buffer); if (r) w->cancel(w, buffer); }
            else if (buffer) w->cancel(w, buffer);
        }
    }
    pthread_mutex_unlock(&s->mutex);
    return r;
}
static void callbacks(cam_device *d, cam_notify n, cam_data data, cam_timestamp ts, cam_allocate a, void *user) {
    (void)ts;
    camera_state *s = state(d); pthread_mutex_lock(&s->mutex);
    s->notify = n; s->data = data; s->allocate = a; s->user = user;
    pthread_mutex_unlock(&s->mutex);
}
static void enable(cam_device *d, int messages) {
    camera_state *s = state(d); pthread_mutex_lock(&s->mutex); s->messages |= messages; pthread_mutex_unlock(&s->mutex);
}
static void disable(cam_device *d, int messages) {
    camera_state *s = state(d); pthread_mutex_lock(&s->mutex); s->messages &= ~messages; pthread_mutex_unlock(&s->mutex);
}
static int enabled(cam_device *d, int messages) {
    camera_state *s = state(d); pthread_mutex_lock(&s->mutex);
    int r = (s->messages & messages) == messages; pthread_mutex_unlock(&s->mutex); return r;
}
static void notify(camera_state *s, int message, int ext1) {
    pthread_mutex_lock(&s->mutex);
    cam_notify cb = s->notify; void *user = s->user; int on = s->messages & message;
    pthread_mutex_unlock(&s->mutex);
    if (cb && on) cb(message, ext1, 0, user);
}
static void deliver(camera_state *s, int message, const unsigned char *data, unsigned size) {
    pthread_mutex_lock(&s->mutex);
    cam_allocate allocate = s->allocate; cam_data cb = s->data; void *user = s->user; int on = s->messages & message;
    pthread_mutex_unlock(&s->mutex);
    if (!allocate || !cb || !on) return;
    cam_memory *memory = allocate(-1, size, 1, user);
    if (memory) {
        if (memory->data && memory->size >= size) { memcpy(memory->data, data, size); cb(message, memory, 0, 0, user); }
        if (memory->release) memory->release(memory);
    }
}
static unsigned char *jpeg_with_orientation(const unsigned char *jpeg, unsigned size, int rotation, unsigned *out_size) {
    if (size < 2 || jpeg[0] != 0xff || jpeg[1] != 0xd8) return 0;
    unsigned char *out = malloc(size + 36);
    if (!out) return 0;
    static const unsigned char exif[38] = {
        0xff,0xd8,0xff,0xe1,0,0x22,'E','x','i','f',0,0,
        'I','I',42,0,8,0,0,0,1,0,0x12,1,3,0,1,0,0,0,1,0,0,0,0,0,0,0
    };
    memcpy(out, exif, sizeof(exif));
    out[30] = rotation == 90 ? 6 : rotation == 180 ? 3 : rotation == 270 ? 8 : 1;
    memcpy(out + 38, jpeg + 2, size - 2); *out_size = size + 36;
    return out;
}
static int snapshot(camera_state *s) {
    pthread_mutex_lock(&s->mutex); int quality = s->quality, rotation = s->rotation; pthread_mutex_unlock(&s->mutex);
    int fd = cam_connect(2, s->id, quality);
    if (fd < 0) return fd;
    unsigned char header[16]; unsigned char *jpeg = 0; int r = cam_io(fd, header, 16, 0, 2000);
    if (!r && !memcmp(header, "ERR1", 4)) r = (int)cam_le32(header + 4);
    if (!r && memcmp(header, "JPG1", 4)) r = -EPROTO;
    unsigned size = !r ? cam_le32(header + 4) : 0;
    if (!r && (size < 2 || size > CAM_MAX_JPEG)) r = -EPROTO;
    if (!r) { jpeg = malloc(size); if (!jpeg) r = -ENOMEM; }
    if (!r) r = cam_io(fd, jpeg, size, 0, 2000);
    close(fd);
    if (!r && running(s)) {
        unsigned final_size = 0;
        unsigned char *final = jpeg_with_orientation(jpeg, size, rotation, &final_size);
        if (!final) r = -EPROTO;
        else {
            pthread_mutex_lock(&s->mutex); int capture = s->capture; s->capture = 0; pthread_mutex_unlock(&s->mutex);
            if (capture) { notify(s, MSG_SHUTTER, 0); notify(s, MSG_RAW_NOTIFY, 0); deliver(s, MSG_JPEG, final, final_size); }
            free(final);
        }
    }
    free(jpeg); return r;
}
static void *preview_thread(void *arg) {
    camera_state *s = arg;
    pthread_mutex_lock(&s->mutex); s->execution = pthread_self(); s->in_worker = 1; pthread_mutex_unlock(&s->mutex);
    unsigned char *frame = malloc(CAM_BYTES); int r = frame ? 0 : -ENOMEM;
    while (!r && running(s)) {
        unsigned char header[16];
        r = cam_io(s->fd, header, 16, 0, 5000);
        if (!r && !memcmp(header, "ERR1", 4)) r = (int)cam_le32(header + 4);
        if (!r && (memcmp(header, "FRM1", 4) || cam_le32(header + 4) != CAM_BYTES)) r = -EPROTO;
        if (!r) r = cam_io(s->fd, frame, CAM_BYTES, 0, 2000);
        if (r || !running(s)) break;
        r = render(s, frame);
        if (r) break;
        deliver(s, MSG_PREVIEW, frame, CAM_BYTES);
        pthread_mutex_lock(&s->mutex); int capture = s->capture; pthread_mutex_unlock(&s->mutex);
        if (capture) { r = snapshot(s); break; } // legacy takePicture stops preview
    }
    if (r && running(s)) notify(s, MSG_ERROR, 1 /* CAMERA_ERROR_UNKNOWN */);
    __atomic_store_n(&s->running, 0, __ATOMIC_RELEASE);
    pthread_mutex_lock(&s->mutex);
    if (s->fd >= 0) { close(s->fd); s->fd = -1; }
    s->in_worker = 0;
    pthread_mutex_unlock(&s->mutex);
    free(frame); return 0;
}
static void stop_internal(cam_device *d) {
    camera_state *s = state(d);
    __atomic_store_n(&s->running, 0, __ATOMIC_RELEASE);
    pthread_mutex_lock(&s->mutex);
    if (s->fd >= 0) shutdown(s->fd, SHUT_RDWR);
    int started = s->started; pthread_t thread = s->thread;
    pthread_mutex_unlock(&s->mutex);
    if (started && !pthread_equal(thread, pthread_self())) {
        pthread_join(thread, 0);
        pthread_mutex_lock(&s->mutex); s->started = 0; s->capture = 0; pthread_mutex_unlock(&s->mutex);
    }
}
static void stop_preview(cam_device *d) {
    camera_state *s = state(d);
    // A callback may stop its own preview: don't wait behind a caller joining this thread.
    if (worker_is_self(s)) { stop_internal(d); return; }
    pthread_mutex_lock(&s->control); stop_internal(d); pthread_mutex_unlock(&s->control);
}
static int start_preview(cam_device *d) {
    camera_state *s = state(d);
    if (worker_is_self(s)) return -EBUSY;
    pthread_mutex_lock(&s->control);
    if (running(s)) { pthread_mutex_unlock(&s->control); return 0; }
    stop_internal(d);
    int fd = cam_connect(1, s->id, 0);
    if (fd < 0) { pthread_mutex_unlock(&s->control); return fd; }
    pthread_mutex_lock(&s->mutex);
    s->fd = fd; s->capture = 0; __atomic_store_n(&s->running, 1, __ATOMIC_RELEASE);
    int r = pthread_create(&s->thread, 0, preview_thread, s);
    if (r) { __atomic_store_n(&s->running, 0, __ATOMIC_RELEASE); close(fd); s->fd = -1; }
    else s->started = 1;
    pthread_mutex_unlock(&s->mutex);
    pthread_mutex_unlock(&s->control);
    return -r;
}
static int preview_enabled(cam_device *d) { return running(state(d)); }
static int metadata(cam_device *d, int on) { (void)d; return on ? -ENOSYS : 0; }
static int record(cam_device *d) { (void)d; return -ENOSYS; }
static void stop_record(cam_device *d) { (void)d; }
static int recording(cam_device *d) { (void)d; return 0; }
static void release_frame(cam_device *d, const void *frame) { (void)d; (void)frame; }
static int focus(cam_device *d) { notify(state(d), MSG_FOCUS, 1); return 0; } // advertised fixed focus
static int cancel_focus(cam_device *d) { (void)d; return 0; }
static int take_picture(cam_device *d) {
    camera_state *s = state(d); if (!running(s)) return -ENODEV;
    pthread_mutex_lock(&s->mutex); s->capture = 1; pthread_mutex_unlock(&s->mutex); return 0;
}
static int cancel_picture(cam_device *d) {
    camera_state *s = state(d); pthread_mutex_lock(&s->mutex); s->capture = 0; pthread_mutex_unlock(&s->mutex); return 0;
}
static int value(const char *params, const char *key, char *out, unsigned capacity) {
    size_t k = strlen(key);
    for (const char *p = params; p && *p; ) {
        const char *end = strchr(p, ';'); if (!end) end = p + strlen(p);
        if ((size_t)(end - p) > k && !memcmp(p, key, k) && p[k] == '=') {
            unsigned n = end - p - k - 1;
            if (n >= capacity) return -1;
            memcpy(out, p + k + 1, n); out[n] = 0; return 1;
        }
        p = *end ? end + 1 : end;
    }
    return 0;
}
static int set_parameters(cam_device *d, const char *params) {
    if (!params || strlen(params) > 16384) return -EINVAL;
    static const char *keys[] = {"preview-size","picture-size","preview-format","picture-format","focus-mode","flash-mode","zoom","preview-frame-rate","preview-fps-range"};
    static const char *values[] = {"640x480","640x480","yuv420sp","jpeg","fixed","off","0","15","15000,15000"};
    char v[64];
    for (unsigned i = 0; i < sizeof(keys)/sizeof(keys[0]); i++) {
        int found = value(params, keys[i], v, sizeof(v));
        if (found < 0 || (found && strcmp(v, values[i]))) return -EINVAL;
    }
    camera_state *s = state(d);
    pthread_mutex_lock(&s->mutex); int rotation = s->rotation, quality = s->quality; pthread_mutex_unlock(&s->mutex);
    int found = value(params, "rotation", v, sizeof(v));
    if (found < 0) return -EINVAL;
    if (found) {
        char *end; long number = strtol(v, &end, 10);
        if (!*v || *end || number < 0 || number > 270 || number % 90) return -EINVAL;
        rotation = number;
    }
    found = value(params, "jpeg-quality", v, sizeof(v));
    if (found < 0) return -EINVAL;
    if (found) { char *end; long number = strtol(v, &end, 10); if (!*v || *end || number < 1 || number > 100) return -EINVAL; quality = number; }
    pthread_mutex_lock(&s->mutex); s->rotation = rotation; s->quality = quality; pthread_mutex_unlock(&s->mutex);
    return 0;
}
static char *get_parameters(cam_device *d) {
    camera_state *s = state(d); char *p = malloc(2048); if (!p) return 0;
    pthread_mutex_lock(&s->mutex); int rotation = s->rotation, quality = s->quality; pthread_mutex_unlock(&s->mutex);
    snprintf(p, 2048, "preview-size=640x480;preview-size-values=640x480;picture-size=640x480;picture-size-values=640x480;"
        "preview-format=yuv420sp;preview-format-values=yuv420sp;picture-format=jpeg;picture-format-values=jpeg;"
        "preview-frame-rate=15;preview-frame-rate-values=15;preview-fps-range=15000,15000;preview-fps-range-values=(15000,15000);"
        "jpeg-quality=%d;jpeg-thumbnail-width=0;jpeg-thumbnail-height=0;jpeg-thumbnail-size-values=0x0;rotation=%d;"
        "focal-length=3.5;horizontal-view-angle=60;vertical-view-angle=45;"
        "focus-mode=fixed;focus-mode-values=fixed;flash-mode=off;flash-mode-values=off;zoom=0;max-zoom=0;zoom-supported=false;"
        "video-size=640x480;video-size-values=640x480;preferred-preview-size-for-video=640x480;"
        "video-frame-format=yuv420sp;video-snapshot-supported=false;recording-hint=false;whitebalance=auto;whitebalance-values=auto;scene-mode=auto;scene-mode-values=auto;"
        "effect=none;effect-values=none;antibanding=auto;antibanding-values=auto;exposure-compensation=0;min-exposure-compensation=0;"
        "max-exposure-compensation=0;exposure-compensation-step=0;auto-exposure-lock-supported=false;auto-whitebalance-lock-supported=false;"
        "max-num-detected-faces-hw=0;max-num-detected-faces-sw=0;"
        /* Sony's launcher unconditionally selects SCENE_RECOGNITION, but its
         * ParameterManager only creates that mode when this key is true.
         * Expose the automatic-mode shell, using our existing host automatic
         * capture. No detected scenes or proprietary enhancements are claimed.
         * Leave the extension version empty: a nonempty version makes Sony
         * instantiate its proprietary CameraExtension service, which this
         * generic HAL does not implement. No scene-apply types are supported. */
        "sony-scene-detect-supported=true;sony-scene-detect-apply-types=;sony-extension-version=", quality, rotation);
    return p;
}
static void put_parameters(cam_device *d, char *params) { (void)d; free(params); }
static int command(cam_device *d, int cmd, int a1, int a2) {
    (void)d; (void)a2;
    return cmd == 3 && a1 >= 0 && a1 <= 270 && a1 % 90 == 0 ? 0 : -ENOSYS;
}
static void release(cam_device *d) { stop_preview(d); }
static int dump(cam_device *d, int fd) { (void)d; (void)fd; return 0; }
static int close_camera(cam_device_common *d) {
    camera_state *s = state((cam_device *)d);
    if (worker_is_self(s)) return -EBUSY;
    stop_preview(&s->device); pthread_mutex_destroy(&s->control); pthread_mutex_destroy(&s->mutex); free(s); return 0;
}
static cam_ops operations = {
    set_window, callbacks, enable, disable, enabled, start_preview, stop_preview, preview_enabled,
    metadata, record, stop_record, recording, release_frame, focus, cancel_focus,
    take_picture, cancel_picture, set_parameters, get_parameters, put_parameters, command, release, dump
};
static int open_camera(const cam_module_common *module, const char *name, cam_device_common **out) {
    if (!out || !name || name[0] < '0' || name[0] > '1' || name[1]) return -EINVAL;
    int id = name[0] - '0'; if (id >= count_cameras()) return -ENODEV;
    camera_state *s = calloc(1, sizeof(*s)); if (!s) return -ENOMEM;
    int r = pthread_mutex_init(&s->mutex, 0); if (r) { free(s); return -r; }
    r = pthread_mutex_init(&s->control, 0); if (r) { pthread_mutex_destroy(&s->mutex); free(s); return -r; }
    s->device.common.tag = 0x48574454; s->device.common.version = 0x100;
    s->device.common.module = (cam_module_common *)module; s->device.common.close = close_camera;
    s->device.ops = &operations; s->device.priv = s;
    s->id = id; s->fd = -1; s->quality = 85;
    *out = &s->device.common; return 0;
}
static cam_methods methods = { open_camera };
EXPORT cam_module HMI = {
    .common = { .tag = 0x48574d54, .version = 0x100, .hal_version = 0,
        .id = "camera", .name = "AEmulator host camera", .author = "AEmulator Sunset", .methods = &methods },
    .count = count_cameras, .info = get_info
};
