#include <errno.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <poll.h>
#include <string.h>
#include <time.h>
#ifndef CAMERA_SOCKET
#define CAMERA_SOCKET "/dev/aemu_camera"
#endif
#define CAM_WIDTH 640
#define CAM_HEIGHT 480
#define CAM_BYTES (CAM_WIDTH * CAM_HEIGHT * 3 / 2)
#define CAM_MAX_JPEG (2 * 1024 * 1024)
static unsigned cam_le32(const unsigned char *p) {
    return (unsigned)p[0] | ((unsigned)p[1] << 8) | ((unsigned)p[2] << 16) | ((unsigned)p[3] << 24);
}
static int64_t cam_millis(void) {
    struct timespec now;
    if (clock_gettime(CLOCK_MONOTONIC, &now)) return -1;
    return (int64_t)now.tv_sec * 1000 + now.tv_nsec / 1000000;
}
static int cam_io(int fd, void *data, unsigned size, int writing, unsigned timeout_ms) {
    unsigned offset = 0;
    int64_t start = cam_millis();
    if (start < 0) return -errno;
    int64_t deadline = start + timeout_ms;
    struct pollfd p = { fd, writing ? POLLOUT : POLLIN, 0 };
    while (offset < size) {
        int64_t now = cam_millis();
        if (now < 0) return -errno;
        if (now >= deadline) return -ETIMEDOUT;
        int wait = deadline - now < 50 ? (int)(deadline - now) : 50;
        int r = poll(&p, 1, wait);
        if (r < 0 && errno == EINTR) continue;
        if (r < 0) return -errno;
        if (!r) continue;
        ssize_t n = writing ? send(fd, (char *)data + offset, size - offset, MSG_NOSIGNAL)
                            : read(fd, (char *)data + offset, size - offset);
        if (n < 0 && (errno == EAGAIN || errno == EINTR)) continue;
        if (n <= 0) return n < 0 ? -errno : -EPIPE;
        offset += n;
    }
    return offset == size ? 0 : -ETIMEDOUT;
}
static int cam_connect(unsigned op, unsigned id, unsigned quality) {
    struct sockaddr_un addr;
    memset(&addr, 0, sizeof(addr)); addr.sun_family = AF_UNIX;
    memcpy(addr.sun_path, CAMERA_SOCKET, sizeof(CAMERA_SOCKET));
    int fd = socket(AF_UNIX, SOCK_STREAM | SOCK_CLOEXEC | SOCK_NONBLOCK, 0);
    if (fd < 0) return -errno;
    if (connect(fd, (struct sockaddr *)&addr, sizeof(addr)) < 0) { int e = -errno; close(fd); return e; }
    unsigned char packet[16] = { 'C', 'A', 'M', '1' };
    for (unsigned i = 0; i < 4; i++) {
        packet[4 + i] = op >> (8 * i); packet[8 + i] = id >> (8 * i); packet[12 + i] = quality >> (8 * i);
    }
    int r = cam_io(fd, packet, sizeof(packet), 1, 1000);
    if (r < 0) { close(fd); return r; }
    return fd;
}
