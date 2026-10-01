#define _GNU_SOURCE
#define CAMERA_SOCKET "camera-test.sock"
#include "../camera_hal.c"
#include <sys/wait.h>
#include <time.h>
#define CHECK(c) do { if (!(c)) { fprintf(stderr, "camera smoke failed at line %d\n", __LINE__); return 1; } } while (0)
static int read_full(int fd, void *p, size_t n) {
    size_t got = 0;
    while (got < n) { ssize_t r = read(fd, (char *)p + got, n - got); if (r <= 0) return -1; got += r; }
    return 0;
}
static int write_full(int fd, const void *p, size_t n) {
    size_t got = 0;
    while (got < n) { ssize_t r = send(fd, (const char *)p + got, n - got, MSG_NOSIGNAL); if (r <= 0) return -1; got += r; }
    return 0;
}
static int fake_host(int server) {
    for (unsigned op = 0; op < 3; op++) {
        int peer = accept(server, 0, 0); CHECK(peer >= 0);
        unsigned char req[16]; CHECK(!read_full(peer, req, 16));
        CHECK(!memcmp(req, "CAM1", 4) && cam_le32(req + 4) == op && cam_le32(req + 8) == 0);
        if (op == 0) {
            unsigned char metadata[24] = {'I','N','F','1',2,0,0,0,0,0,0,0,90,0,0,0,1,0,0,0,14,1,0,0};
            CHECK(!write_full(peer, metadata, 24)); close(peer);
        } else if (op == 1) {
            unsigned char header[16] = {'F','R','M','1'};
            for (unsigned i = 0; i < 4; i++) header[4 + i] = CAM_BYTES >> (8*i);
            unsigned char *frame = malloc(CAM_BYTES); CHECK(frame);
            memset(frame, 128, CAM_BYTES);
            CHECK(!write_full(peer, header, 16) && !write_full(peer, frame, CAM_BYTES));
            free(frame);
            // Keep preview socket alive while the snapshot uses a separate connection.
        } else {
            CHECK(cam_le32(req + 12) == 90);
            unsigned char response[20] = {'J','P','G','1',4,0,0,0,0,0,0,0,0,0,0,0,0xff,0xd8,0xff,0xd9};
            CHECK(!write_full(peer, response, 20)); close(peer);
        }
    }
    return 0;
}
static unsigned char pixels[CAM_WIDTH * CAM_HEIGHT * 4];
static const void *handle = pixels;
static int geometry(cam_window *w, int width, int height, int format) { (void)w; return width == CAM_WIDTH && height == CAM_HEIGHT && format == 1 ? 0 : -EINVAL; }
static int dequeue(cam_window *w, const void ***buffer, int *stride) { (void)w; *buffer = &handle; *stride = CAM_WIDTH; return 0; }
static int enqueue(cam_window *w, const void **buffer) { (void)w; return *buffer == pixels ? 0 : -EINVAL; }
static int cancel(cam_window *w, const void **buffer) { (void)w; (void)buffer; return 0; }
static int lock_pixels(const void *m, const void *h, int usage, int x, int y, int width, int height, void **out) {
    (void)m; (void)usage; (void)x; (void)y; (void)width; (void)height;
    if (h != pixels) return -EINVAL; *out = pixels; return 0;
}
static int unlock_pixels(const void *m, const void *h) { (void)m; return h == pixels ? 0 : -EINVAL; }
static cam_gralloc fake_gralloc = { .lock = lock_pixels, .unlock = unlock_pixels };
static cam_window window = { .geometry = geometry, .dequeue = dequeue, .enqueue = enqueue, .cancel = cancel };
typedef struct {
    cam_device *device;
    pthread_mutex_t mutex;
    pthread_cond_t cond;
    int preview, jpeg, error, focus, allocated, freed, bad;
} test_state;
static test_state test = { .mutex = PTHREAD_MUTEX_INITIALIZER, .cond = PTHREAD_COND_INITIALIZER };
static void release_memory(cam_memory *m) { pthread_mutex_lock(&test.mutex); test.freed++; pthread_mutex_unlock(&test.mutex); free(m->data); free(m); }
static cam_memory *allocate_memory(int fd, size_t size, unsigned n, void *user) {
    (void)user; if (fd != -1 || n != 1) return 0;
    cam_memory *m = calloc(1, sizeof(*m)); if (!m) return 0;
    m->data = malloc(size); m->size = size; m->release = release_memory;
    if (!m->data) { free(m); return 0; }
    pthread_mutex_lock(&test.mutex); test.allocated++; pthread_mutex_unlock(&test.mutex);
    return m;
}
static void notify_callback(int message, int ext1, int ext2, void *user) {
    (void)ext2; (void)user;
    pthread_mutex_lock(&test.mutex);
    if (message == MSG_ERROR) test.error++;
    if (message == MSG_FOCUS && ext1 == 1) test.focus++;
    pthread_cond_signal(&test.cond); pthread_mutex_unlock(&test.mutex);
}
static void data_callback(int message, const cam_memory *memory, unsigned index, void *metadata, void *user) {
    (void)index; (void)metadata; (void)user;
    pthread_mutex_lock(&test.mutex);
    if (message == MSG_PREVIEW) {
        test.preview++;
        if (memory->size != CAM_BYTES || ((unsigned char *)memory->data)[0] != 128) test.bad++;
    }
    if (message == MSG_JPEG) {
        test.jpeg++;
        unsigned char *p = memory->data;
        if (memory->size != 40 || p[0] != 0xff || p[1] != 0xd8 || p[30] != 6 || p[38] != 0xff || p[39] != 0xd9) test.bad++;
    }
    pthread_cond_signal(&test.cond); pthread_mutex_unlock(&test.mutex);
    if (message == MSG_PREVIEW) test.device->ops->picture(test.device); // callback re-entry
}
int main(void) {
    // Slow, fragmented transport must use a wall-clock deadline, not a read-count limit.
    int pair[2]; CHECK(!socketpair(AF_UNIX, SOCK_STREAM, 0, pair));
    pid_t fragment_child = fork(); CHECK(fragment_child >= 0);
    if (!fragment_child) {
        close(pair[0]); unsigned char chunk[512]; memset(chunk, 0x5a, sizeof(chunk));
        for (unsigned i = 0; i < 100; i++) {
            if (write_full(pair[1], chunk, sizeof(chunk))) _exit(1);
            usleep(2000);
        }
        close(pair[1]); _exit(0);
    }
    close(pair[1]); unsigned char fragmented[51200];
    CHECK(!cam_io(pair[0], fragmented, sizeof(fragmented), 0, 1000));
    for (unsigned i = 0; i < sizeof(fragmented); i++) CHECK(fragmented[i] == 0x5a);
    int fragment_status; CHECK(waitpid(fragment_child, &fragment_status, 0) == fragment_child && fragment_status == 0);
    CHECK(cam_io(pair[0], fragmented, 1, 0, 100) == -EPIPE); close(pair[0]);
    CHECK(!socketpair(AF_UNIX, SOCK_STREAM, 0, pair));
    CHECK(cam_io(pair[0], fragmented, 1, 0, 80) == -ETIMEDOUT);
    close(pair[0]); close(pair[1]);
    CHECK(count_cameras() == 0); // absent host bridge: no invented cameras
    int server = socket(AF_UNIX, SOCK_STREAM, 0); CHECK(server >= 0);
    struct sockaddr_un addr; memset(&addr, 0, sizeof(addr)); addr.sun_family = AF_UNIX;
    memcpy(addr.sun_path, CAMERA_SOCKET, sizeof(CAMERA_SOCKET));
    CHECK(!bind(server, (struct sockaddr *)&addr, sizeof(addr)) && !listen(server, 4));
    pid_t child = fork(); CHECK(child >= 0);
    if (!child) _exit(fake_host(server));
    CHECK(HMI.common.tag == 0x48574d54 && HMI.common.version == 0x100);
    CHECK(HMI.count() == 2);
    int info[4] = {-1,-1,0x12345678,0x12345678};
    CHECK(!HMI.info(1, info) && info[0] == 1 && info[1] == 270 && info[2] == 0x12345678);
    CHECK(HMI.info(2, info) == -EINVAL);
    cam_device_common *common = 0;
    CHECK(HMI.common.methods->open(&HMI.common, "2", &common) == -EINVAL);
    CHECK(!HMI.common.methods->open(&HMI.common, "0", &common));
    test.device = (cam_device *)common;
    cam_ops *ops = test.device->ops;
    CHECK(ops->record(test.device) == -ENOSYS && ops->recording(test.device) == 0);
    CHECK(ops->set_parameters(test.device, "preview-size=1920x1080") == -EINVAL);
    CHECK(ops->set_parameters(test.device, "rotation=45") == -EINVAL);
    CHECK(!ops->set_parameters(test.device, "preview-size=640x480;rotation=90;jpeg-quality=90"));
    char *parameters = ops->get_parameters(test.device); CHECK(parameters && strstr(parameters, "jpeg-quality=90") && strstr(parameters, "rotation=90"));
    ops->put_parameters(test.device, parameters);
    gralloc = &fake_gralloc; CHECK(!ops->set_window(test.device, &window));
    ops->callbacks(test.device, notify_callback, data_callback, 0, allocate_memory, &test);
    ops->enable(test.device, MSG_PREVIEW | MSG_JPEG | MSG_ERROR | MSG_FOCUS | MSG_SHUTTER);
    CHECK(ops->enabled(test.device, MSG_PREVIEW | MSG_JPEG));
    CHECK(!ops->focus(test.device));
    CHECK(!ops->start(test.device));
    struct timespec deadline; clock_gettime(CLOCK_REALTIME, &deadline); deadline.tv_sec += 8;
    pthread_mutex_lock(&test.mutex);
    while (!test.jpeg && !test.error) if (pthread_cond_timedwait(&test.cond, &test.mutex, &deadline) == ETIMEDOUT) break;
    pthread_mutex_unlock(&test.mutex);
    ops->stop(test.device);
    CHECK(test.preview == 1 && test.jpeg == 1 && test.error == 0 && test.focus == 1 && test.bad == 0);
    CHECK(test.allocated == 2 && test.freed == 2);
    CHECK(pixels[0] >= 129 && pixels[0] <= 131 && pixels[0] == pixels[1] && pixels[1] == pixels[2] && pixels[3] == 255);
    CHECK(!ops->previewing(test.device) && !common->close(common));
    int status; CHECK(waitpid(child, &status, 0) == child && status == 0);
    close(server); unlink(CAMERA_SOCKET);
    puts("camera HAL smoke OK"); return 0;
}
