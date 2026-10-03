#define SENSOR_SOCKET "sensor.sock"
#include "../sensors_hal.c"
#include <assert.h>
#include <stdio.h>
static int listener;
static void *host(void *unused) {
    (void)unused; int64_t time=1000000000;
    for(;;) {
        int fd=accept(listener,0,0); if(fd<0) break;
        unsigned char req[12]; assert(!transfer(fd,req,12,0));
        assert(!memcmp(req,"SNS1",4));
        unsigned char response[92]={0};
        if(le32(req+4)==0) { memcpy(response,"SNI1",4); put32(response+4,5); assert(!transfer(fd,response,8,1)); }
        else {
            memcpy(response,"SNE1",4); unsigned count=0;
            for(int i=0;i<3;i++) if((le32(req+8)&5)&(1<<i)) {
                unsigned char *p=response+8+count++*28; put32(p,types[i]);
                put32(p+4,(unsigned)time); put32(p+8,(uint64_t)time>>32);
                float values[3]={1,2,9.81f}; memcpy(p+12,values,12); put32(p+24,3);
            }
            time+=50000000; put32(response+4,count); assert(!transfer(fd,response,8+count*28,1));
        }
        close(fd);
    }
    return 0;
}
static void *poll_wait(void *dev) { event e; assert(polling(dev,&e,1)==-ECANCELED); return 0; }
int main(void) {
    listener=socket(AF_UNIX,SOCK_STREAM,0); assert(listener>=0);
    struct sockaddr_un addr={0}; addr.sun_family=AF_UNIX; strcpy(addr.sun_path,SENSOR_SOCKET);
    assert(!bind(listener,(struct sockaddr *)&addr,sizeof(addr))); assert(!listen(listener,8));
    pthread_t thread; assert(!pthread_create(&thread,0,host,0));
    const sensor *sensors; assert(sensors_list(&HMI.common,&sensors)==2);
    assert(sensors[0].type==1 && sensors[1].type==4 && sensors[1].handle==3);
    device *d; assert(!opening(&HMI.common,"poll",&d));
    poll_device *p=(poll_device *)d;
    assert(activate(p,2,1)==-EINVAL); assert(delay(p,1,-1)==-EINVAL);
    assert(!activate(p,1,1)); assert(!activate(p,3,1));
    event events[2]; assert(polling(p,events,2)==2); assert(events[0].sensor==1 && events[1].sensor==3);
    assert(events[0].time>0 && events[0].data[2]>9.8f);
    assert(polling(p,events,1)==1); // count is bounded by caller's capacity
    assert(!activate(p,1,0)); assert(!activate(p,3,0));
    pthread_t blocked; assert(!pthread_create(&blocked,0,poll_wait,p));
    int ready=0;
    for(int n=0;n<1000 && !ready;n++) {
        state *current=(state *)p; pthread_mutex_lock(&current->lock); ready=current->readers>0; pthread_mutex_unlock(&current->lock);
        if(!ready) usleep(1000);
    }
    assert(ready); assert(!closing(d)); pthread_join(blocked,0);
    shutdown(listener,SHUT_RDWR); close(listener); pthread_join(thread,0); unlink(SENSOR_SOCKET);
    puts("sensor HAL smoke passed: mask, ABI callbacks, samples, capacity, close wakeup");
    return 0;
}
