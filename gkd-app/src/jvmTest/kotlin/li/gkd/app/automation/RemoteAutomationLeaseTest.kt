package li.gkd.app.automation

import kotlinx.serialization.json.Json
import li.gkd.app.settings.SettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoteAutomationLeaseTest {
    @Test
    fun legacyPersistedSettingsKeepRemoteControlDisabledAndNewSettingRoundTrips() {
        val old = Json.decodeFromString<SettingsStore>("""{"enableAutomator":true,"automatorMode":2}""")
        assertFalse(old.enableRemoteAutomationControl)
        assertTrue(old.enableAutomator)
        assertEquals(2, old.automatorMode)
        val updated = old.copy(enableRemoteAutomationControl = true)
        assertEquals(updated, Json.decodeFromString<SettingsStore>(Json.encodeToString(updated)))
    }

    @Test
    fun heartbeatExtendsDeadlineButCannotReviveAnExpiredLease() {
        val lease = RemoteAutomationLease()
        assertTrue(lease.acquire("task-a", 100L, true))
        assertTrue(lease.renew("task-a", 20_000L))
        assertFalse(lease.expire(30_100L))
        assertFalse(lease.renew("task-a", 50_000L))
        assertTrue(lease.expire(50_000L))
        assertFalse(lease.paused)
        assertTrue(lease.shouldRestore)
    }

    @Test
    fun oldTasksCannotRenewOrReleaseAnotherTasksPause() {
        val lease = RemoteAutomationLease()
        assertTrue(lease.acquire("task-a", 0L, true))
        assertFalse(lease.acquire("task-b", 1L, true))
        assertFalse(lease.renew("task-b", 1L))
        assertFalse(lease.release("task-b"))
        assertTrue(lease.release("task-a"))
        assertTrue(lease.acquire("task-b", 2L, false))
        assertFalse(lease.release("task-a"))
        assertTrue(lease.paused)
        assertTrue(lease.shouldRestore)
    }

    @Test
    fun duplicatePausePreservesOriginalRestoreIntent() {
        val lease = RemoteAutomationLease()
        lease.acquire("task-a", 0L, true)
        lease.acquire("task-a", 10L, false)
        lease.release("task-a")
        assertTrue(lease.shouldRestore)
        lease.cancelRestore()
        assertFalse(lease.shouldRestore)
    }

    @Test
    fun disablingFeatureEndsPauseWithoutInventingAnEnabledService() {
        val lease = RemoteAutomationLease()
        lease.acquire("task-a", 0L, false)
        lease.releaseAll()
        assertFalse(lease.paused)
        assertFalse(lease.shouldRestore)
        assertFalse(lease.renew("task-a", 1L))
    }

    @Test
    fun manualStopCancelsRestorationEvenIfPauseIsRenewed() {
        val lease = RemoteAutomationLease()
        lease.acquire("task-a", 0L, true)
        lease.cancelRestore()
        assertTrue(lease.renew("task-a", 1L))
        lease.acquire("task-a", 2L, false)
        lease.releaseAll()
        assertFalse(lease.shouldRestore)
    }
}
