// Хост-помощник (arm64): ослабляет fdsan в процессе :vm.
// Закрытый libglbridge делает freopen() поверх fd 2, которым на Android 10+ владеет unique_fd,
// и fdsan роняет всё приложение. Переводим fdsan в режим «предупредить один раз».
#include <jni.h>
#include <android/fdsan.h>

JNIEXPORT void JNICALL Java_app_aemu_core_HostNative_relaxFdsan(JNIEnv *env, jclass cls) {
    (void)env; (void)cls;
    android_fdsan_set_error_level(ANDROID_FDSAN_ERROR_LEVEL_DISABLED);
}
