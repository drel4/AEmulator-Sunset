package app.aemu.core

/** Хост-помощник libaemuhost.so (native/hostjni). На Android 8–9 fdsan нет — библиотека не грузится, это не ошибка. */
object HostNative {
    private val ok: Boolean = try { System.loadLibrary("aemuhost"); true } catch (_: Throwable) { false }

    @JvmStatic private external fun relaxFdsan()

    /** Закрытый GL-мост делает freopen() поверх fd 2 — без этого fdsan роняет процесс :vm. */
    fun relaxFdsanSafe() { if (ok) try { relaxFdsan() } catch (_: Throwable) {} }
}
