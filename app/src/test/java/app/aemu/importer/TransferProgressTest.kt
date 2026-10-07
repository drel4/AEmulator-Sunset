package app.aemu.importer

import org.junit.Assert.*
import org.junit.Test

class TransferProgressTest {
    @Test fun knownLengthHasPercentageIncludingZero() {
        assertEquals(0, TransferProgress.percent(0f))
        assertEquals(37, TransferProgress.percent(.37f))
        assertEquals(100, TransferProgress.percent(1f))
    }
    @Test fun unknownOrInvalidProgressIsIndeterminate() {
        assertNull(TransferProgress.percent(-1f))
        assertNull(TransferProgress.percent(Float.NaN))
        assertNull(TransferProgress.percent(Float.POSITIVE_INFINITY))
    }
    @Test fun percentageCannotOverflowTheNotificationBar() {
        assertEquals(100, TransferProgress.percent(100f))
    }
    @Test fun staleServiceOrNotificationCannotCancelANewerTask() {
        assertTrue(TransferProgress.canCancel(true, 2, 2))
        assertFalse(TransferProgress.canCancel(true, 2, 1))
        assertFalse(TransferProgress.canCancel(false, 2, 2))
    }
}
