/* AEmulator Sunset, 2026-10-03. GPL-3.0.
 * Independently written legacy sensor HAL ABI / transport; ARM32 Android 2.3-7.1.
 * No vendor sensor drivers; only hardware advertised by the host bridge. */
#include <stdint.h>
#include <stddef.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <pthread.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/time.h>
#include <math.h>
#include <fcntl.h>
#define EXPORT __attribute__((visibility("default")))
#ifndef SENSOR_SOCKET
#define SENSOR_SOCKET "/dev/aemu_sensors"
#endif
typedef struct module module;
typedef struct device device;
typedef struct { int (*open)(const module *, const char *, device **); } methods;
struct module { uint32_t tag; uint16_t version, hal_version; const char *id,*name,*author; methods *methods; void *dso; uint32_t reserved[25]; };
struct device { uint32_t tag,version; module *module; uint32_t reserved[12]; int (*close)(device *); };
typedef struct { const char *name,*vendor; int version,handle,type; float range,resolution,power; int32_t min_delay; void *reserved[8]; } sensor;
typedef struct { int32_t version,sensor,type,reserved; int64_t time; float data[16]; uint32_t tail[4]; } event;
typedef struct poll_device poll_device;
struct poll_device { device common; int (*activate)(poll_device *,int,int); int (*delay)(poll_device *,int,int64_t); int (*poll)(poll_device *,event *,int); };
typedef struct { module common; int (*list)(module *,const sensor **); } sensor_module;
typedef struct { poll_device common; pthread_mutex_t lock; pthread_cond_t changed; int enabled,closing,readers; int64_t delay[3],last[3]; } state;
EXPORT sensor_module HMI;
static pthread_mutex_t list_lock = PTHREAD_MUTEX_INITIALIZER;
static sensor list[3]; static int list_count = -1, supported;
static const int types[3] = {1,2,4};
static const char *names[3] = {"Sunset host accelerometer","Sunset host magnetic field","Sunset host gyroscope"};
static const float ranges[3] = {160.f,2000.f,35.f};
static unsigned le32(const unsigned char *p) { return p[0]|((unsigned)p[1]<<8)|((unsigned)p[2]<<16)|((unsigned)p[3]<<24); }
static int64_t le64(const unsigned char *p) { return (int64_t)((uint64_t)le32(p)|((uint64_t)le32(p+4)<<32)); }
static void put32(unsigned char *p,unsigned x) { for(int i=0;i<4;i++) p[i]=x>>(i*8); }
static int transfer(int fd,void *buf,unsigned size,int writing) {
    unsigned n=0;
    while(n<size) {
        ssize_t r=writing?send(fd,(char *)buf+n,size-n,MSG_NOSIGNAL):read(fd,(char *)buf+n,size-n);
        if(r<0 && errno==EINTR) continue;
        if(r<=0) return r<0?-errno:-EPIPE;
        n+=r;
    }
    return 0;
}
static int request(int info,int mask,unsigned char *out,unsigned size) {
    int fd=socket(AF_UNIX,SOCK_STREAM,0); if(fd<0) return -errno;
    fcntl(fd,F_SETFD,FD_CLOEXEC);
    struct timeval tv={0,250000};
    setsockopt(fd,SOL_SOCKET,SO_RCVTIMEO,&tv,sizeof(tv)); setsockopt(fd,SOL_SOCKET,SO_SNDTIMEO,&tv,sizeof(tv));
    struct sockaddr_un addr; memset(&addr,0,sizeof(addr)); addr.sun_family=AF_UNIX;
    if(sizeof(SENSOR_SOCKET)>sizeof(addr.sun_path)) { close(fd); return -EINVAL; }
    memcpy(addr.sun_path,SENSOR_SOCKET,sizeof(SENSOR_SOCKET));
    if(connect(fd,(struct sockaddr *)&addr,sizeof(addr))) { int e=-errno; close(fd); return e; }
    unsigned char msg[12]={'S','N','S','1'}; put32(msg+4,info?0:1); put32(msg+8,mask);
    int r=transfer(fd,msg,12,1);
    if(!r) r=transfer(fd,out,8,0);
    if(!r && !info) {
        unsigned count=le32(out+4);
        if(memcmp(out,"SNE1",4) || count>3 || size<8+count*28) r=-EPROTO;
        else r=transfer(fd,out+8,count*28,0);
    }
    close(fd); return r;
}
static int sensors_list(module *m,const sensor **out) {
    (void)m; if(!out) return -EINVAL;
    pthread_mutex_lock(&list_lock);
    if(list_count<0) {
        unsigned char b[8];
        if(request(1,0,b,sizeof(b)) || memcmp(b,"SNI1",4) || (le32(b+4)&~7u)) { *out=0; pthread_mutex_unlock(&list_lock); return 0; }
        supported=le32(b+4); list_count=0;
        for(int i=0;i<3;i++) if(supported&(1<<i)) {
            sensor *s=&list[list_count++]; memset(s,0,sizeof(*s));
            s->name=names[i]; s->vendor="AEmulator Sunset"; s->version=1; s->handle=i+1; s->type=types[i];
            s->range=ranges[i]; s->resolution=0.001f; s->power=0.1f; s->min_delay=20000;
        }
    }
    *out=list; int count=list_count; pthread_mutex_unlock(&list_lock); return count;
}
static int activate(poll_device *d,int handle,int enabled) {
    state *s=(state *)d; int i=handle-1;
    if(i<0 || i>=3 || !(supported&(1<<i)) || (enabled!=0 && enabled!=1)) return -EINVAL;
    pthread_mutex_lock(&s->lock);
    if(s->closing) { pthread_mutex_unlock(&s->lock); return -ECANCELED; }
    int was_enabled=(s->enabled>>i)&1;
    if(enabled) s->enabled|=1<<i; else s->enabled&=~(1<<i);
    if(was_enabled!=enabled) s->last[i]=0;
    int mask=s->enabled; pthread_cond_broadcast(&s->changed); pthread_mutex_unlock(&s->lock);
    unsigned char ignored[92]; request(0,mask,ignored,sizeof(ignored)); return 0;
}
static int delay(poll_device *d,int handle,int64_t ns) {
    int i=handle-1; if(i<0 || i>=3 || !(supported&(1<<i)) || ns<0) return -EINVAL;
    state *s=(state *)d; pthread_mutex_lock(&s->lock);
    s->delay[i]=ns<20000000?20000000:ns; pthread_mutex_unlock(&s->lock); return 0;
}
static int polling(poll_device *d,event *out,int capacity) {
    if(!out || capacity<=0) return -EINVAL;
    state *s=(state *)d; int result=-ECANCELED;
    pthread_mutex_lock(&s->lock); s->readers++;
    for(;;) {
        while(!s->enabled && !s->closing) pthread_cond_wait(&s->changed,&s->lock);
        if(s->closing) break;
        int mask=s->enabled; pthread_mutex_unlock(&s->lock);
        unsigned char packet[92]; int r=request(0,mask,packet,sizeof(packet));
        pthread_mutex_lock(&s->lock);
        if(s->closing) break;
        int count=0;
        if(!r) for(unsigned n=0;n<le32(packet+4) && count<capacity;n++) {
            unsigned char *p=packet+8+n*28; int type=le32(p),i=-1;
            for(int k=0;k<3;k++) if(types[k]==type) i=k;
            int64_t timestamp=le64(p+4);
            float xyz[3]; memcpy(xyz,p+12,12);
            if(i<0 || !(s->enabled&(1<<i)) || timestamp<=0 || timestamp<=s->last[i] ||
                (s->last[i] && timestamp-s->last[i]<s->delay[i]) || !isfinite(xyz[0]) || !isfinite(xyz[1]) || !isfinite(xyz[2])) continue;
            event *e=&out[count++]; memset(e,0,sizeof(*e)); e->version=sizeof(*e); e->sensor=i+1; e->type=type; e->time=timestamp;
            memcpy(e->data,xyz,12); unsigned status=le32(p+24); ((unsigned char *)e->data)[12]=status<=3?status:0;
            s->last[i]=timestamp;
        }
        if(count) { result=count; break; }
        pthread_mutex_unlock(&s->lock); usleep(r || !le32(packet+4)?100000:10000); pthread_mutex_lock(&s->lock);
    }
    s->readers--; pthread_cond_broadcast(&s->changed); pthread_mutex_unlock(&s->lock); return result;
}
static int closing(device *d) {
    state *s=(state *)d; pthread_mutex_lock(&s->lock); s->closing=1; pthread_cond_broadcast(&s->changed);
    while(s->readers) pthread_cond_wait(&s->changed,&s->lock);
    pthread_mutex_unlock(&s->lock);
    unsigned char ignored[92]; request(0,0,ignored,sizeof(ignored));
    pthread_cond_destroy(&s->changed); pthread_mutex_destroy(&s->lock); free(s); return 0;
}
static int opening(const module *m,const char *name,device **out) {
    if(!out || !name || strcmp(name,"poll")) return -EINVAL;
    const sensor *unused; sensors_list((module *)m,&unused);
    state *s=calloc(1,sizeof(*s)); if(!s) return -ENOMEM;
    pthread_mutex_init(&s->lock,0); pthread_cond_init(&s->changed,0);
    for(int i=0;i<3;i++) s->delay[i]=20000000;
    s->common.common.tag=0x48574454; s->common.common.version=0x10001; s->common.common.module=(module *)m; s->common.common.close=closing;
    s->common.activate=activate; s->common.delay=delay; s->common.poll=polling; *out=&s->common.common; return 0;
}
static methods module_methods={opening};
EXPORT sensor_module HMI={{0x48574d54,1,0,"sensors","Sunset host motion sensors","AEmulator Sunset",&module_methods,0,{0}},sensors_list};
#if defined(__arm__) && !defined(__aarch64__)
_Static_assert(sizeof(sensor)==68,"ARM32 sensor ABI");
_Static_assert(sizeof(event)==104,"ARM32 sensor event ABI");
_Static_assert(offsetof(poll_device,activate)==64,"ARM32 poll ABI");
#endif
