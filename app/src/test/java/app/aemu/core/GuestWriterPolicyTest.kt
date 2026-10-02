/* AEmulator Sunset addition, 2026-10-02. GPL-3.0; see LICENSE. */
package app.aemu.core

import org.junit.Assert.*
import org.junit.Test

class GuestWriterPolicyTest {
    @Test fun detectsNativeOrphansAndSeparateBridgeProcesses() {
        for (name in listOf("libqemu-arm.so", "libbinderd.so", "libdhdrun.so", "libglserverd.so"))
            assertTrue(GuestWriterPolicy.matches("/lib/$name -L /app/images/a9dyxj/root", "app.aemu", "/app/images"))
        assertTrue(GuestWriterPolicy.matches("app.aemu:binder", "app.aemu", "/app/images"))
        assertTrue(GuestWriterPolicy.matches("app.aemu.clone:gl", "app.aemu.clone", "/clone/images"))
    }
    @Test fun unrelatedProcessesAndOtherVariantAreNotWriters() {
        for (command in listOf("app.aemu", "app.aemu:vm", "app.aemu.clone:gl", "/lib/libqemu.so -L /clone/images/a9dyxj/root", "/lib/tool /app/images/a9dyxj/root"))
            assertFalse(GuestWriterPolicy.matches(command, "app.aemu", "/app/images"))
    }
}
