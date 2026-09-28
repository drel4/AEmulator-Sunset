// Хост-помощник (arm64): ослабляет fdsan в процессе :vm.
// Закрытый libglbridge делает freopen() поверх fd 2, которым на Android 10+ владеет unique_fd,
// и fdsan роняет всё приложение. Переводим fdsan в режим «предупредить один раз».
#include <jni.h>
#include <android/fdsan.h>

JNIEXPORT void JNICALL Java_app_aemu_core_HostNative_relaxFdsan(JNIEnv *env, jclass cls) {
    (void)env; (void)cls;
    android_fdsan_set_error_level(ANDROID_FDSAN_ERROR_LEVEL_DISABLED);
}

// Гостевые процессы наследуют RLIMIT_STACK процесса :vm (у приложения он огромный). bionic 6.0 считает
// низ стека главного потока как «верх [stack] минус RLIMIT_STACK»; с лимитом ~2 ГБ ART получает
// нелепые границы стека и падает. 8 МБ — как у обычного процесса Android.
#include <sys/resource.h>
JNIEXPORT void JNICALL Java_app_aemu_core_HostNative_limitStack(JNIEnv *env, jclass cls) {
    (void)env; (void)cls;
    struct rlimit r;
    if (getrlimit(RLIMIT_STACK, &r) == 0 && (r.rlim_cur == RLIM_INFINITY || r.rlim_cur > 8u << 20)) {
        r.rlim_cur = 8u << 20;
        setrlimit(RLIMIT_STACK, &r);
    }
}
