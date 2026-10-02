/* AEmulator Sunset addition, 2026-10-02. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class VmRestartGateTest {
    private class Watch(var live: Boolean = true, val dieOnRegister: Boolean = false) : ProcessDeathWatch {
        var callback: (() -> Unit)? = null
        var registrations = 0
        var removals = 0
        override val alive get() = live
        override fun register(onDeath: () -> Unit) {
            registrations++; callback = onDeath
            if (dieOnRegister) { live = false; onDeath() }
        }
        override fun unregister() { removals++ }
        fun die() { live = false; callback?.invoke() }
    }
    @Test fun waitsUntilTheOldProcessActuallyExits() = runBlocking {
        val watch = Watch()
        var launched = false
        val job = launch(start = CoroutineStart.UNDISPATCHED) { awaitVmProcessExit(watch); launched = true }
        assertFalse(launched)
        watch.die(); job.join()
        assertTrue(launched); assertEquals(1, watch.removals)
    }
    @Test fun anAlreadyDeadProcessDoesNotNeedAWatcher() = runBlocking {
        val watch = Watch(live = false)
        awaitVmProcessExit(watch)
        assertEquals(0, watch.registrations)
    }
    @Test fun deathDuringRegistrationAndDuplicateNotificationsAreSafe() = runBlocking {
        val watch = Watch(dieOnRegister = true)
        awaitVmProcessExit(watch)
        watch.die(); watch.die()
        assertEquals(1, watch.removals)
    }
    @Test fun cancellationRemovesTheWatcherWithoutLaunching() = runBlocking {
        val watch = Watch()
        var launched = false
        val job = launch(start = CoroutineStart.UNDISPATCHED) { awaitVmProcessExit(watch); launched = true }
        job.cancelAndJoin(); watch.die()
        assertFalse(launched); assertEquals(1, watch.removals)
    }
}
