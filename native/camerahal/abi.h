/* Minimal ARM32 HAL1 ABI definitions. Layout follows AOSP hardware/camera.h,
 * camera_common.h, hardware.h and gralloc.h (Android 4.x). */
#include <stdint.h>
#include <stddef.h>
typedef struct cam_module cam_module;
typedef struct cam_device cam_device;
typedef struct cam_window cam_window;
typedef struct cam_memory cam_memory;
typedef struct cam_module_common cam_module_common;
typedef struct cam_device_common cam_device_common;
typedef struct { int (*open)(const cam_module_common *, const char *, cam_device_common **); } cam_methods;
struct cam_module_common {
    uint32_t tag; uint16_t version, hal_version;
    const char *id, *name, *author;
    cam_methods *methods; void *dso; uint32_t reserved[25];
};
struct cam_device_common {
    uint32_t tag, version; cam_module_common *module; uint32_t reserved[12];
    int (*close)(cam_device_common *);
};
struct cam_memory { void *data; size_t size; void *handle; void (*release)(cam_memory *); };
typedef void (*cam_notify)(int, int, int, void *);
typedef void (*cam_data)(int, const cam_memory *, unsigned, void *, void *);
typedef void (*cam_timestamp)(int64_t, int, const cam_memory *, unsigned, void *);
typedef cam_memory *(*cam_allocate)(int, size_t, unsigned, void *);
struct cam_window {
    int (*dequeue)(cam_window *, const void ***, int *);
    int (*enqueue)(cam_window *, const void **);
    int (*cancel)(cam_window *, const void **);
    int (*buffer_count)(cam_window *, int);
    int (*geometry)(cam_window *, int, int, int);
    int (*crop)(cam_window *, int, int, int, int);
    int (*usage)(cam_window *, int);
    int (*swap_interval)(cam_window *, int);
    int (*min_buffers)(const cam_window *, int *);
    int (*lock_buffer)(cam_window *, const void **);
};
typedef struct {
    int (*set_window)(cam_device *, cam_window *);
    void (*callbacks)(cam_device *, cam_notify, cam_data, cam_timestamp, cam_allocate, void *);
    void (*enable)(cam_device *, int); void (*disable)(cam_device *, int); int (*enabled)(cam_device *, int);
    int (*start)(cam_device *); void (*stop)(cam_device *); int (*previewing)(cam_device *);
    int (*metadata)(cam_device *, int); int (*record)(cam_device *); void (*stop_record)(cam_device *);
    int (*recording)(cam_device *); void (*release_frame)(cam_device *, const void *);
    int (*focus)(cam_device *); int (*cancel_focus)(cam_device *);
    int (*picture)(cam_device *); int (*cancel_picture)(cam_device *);
    int (*set_parameters)(cam_device *, const char *); char *(*get_parameters)(cam_device *);
    void (*put_parameters)(cam_device *, char *);
    int (*command)(cam_device *, int, int, int); void (*release)(cam_device *); int (*dump)(cam_device *, int);
} cam_ops;
struct cam_device { cam_device_common common; cam_ops *ops; void *priv; };
struct cam_module { cam_module_common common; int (*count)(void); int (*info)(int, void *); void *reserved[8]; };
typedef struct {
    cam_module_common common;
    int (*register_buffer)(const void *, const void *);
    int (*unregister_buffer)(const void *, const void *);
    int (*lock)(const void *, const void *, int, int, int, int, int, void **);
    int (*unlock)(const void *, const void *);
} cam_gralloc;
#if __SIZEOF_POINTER__ == 4
_Static_assert(sizeof(cam_module_common) == 0x80, "module ABI");
_Static_assert(sizeof(cam_device_common) == 0x40, "device ABI");
_Static_assert(offsetof(cam_module, count) == 0x80, "camera count slot");
_Static_assert(offsetof(cam_module, info) == 0x84, "camera info slot");
_Static_assert(offsetof(cam_device, ops) == 0x40, "camera ops slot");
_Static_assert(offsetof(cam_ops, get_parameters) == 0x48, "camera parameter slot");
_Static_assert(offsetof(cam_gralloc, lock) == 0x88, "gralloc lock slot");
#endif
