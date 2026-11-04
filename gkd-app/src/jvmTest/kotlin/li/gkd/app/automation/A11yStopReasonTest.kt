package li.gkd.app.automation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import li.gkd.app.settings.AppScopePolicy
import li.gkd.app.settings.AutomatorMode
import li.gkd.app.settings.SettingsStore

class A11yStopReasonTest {
    @Test
    fun systemDisabledHelperPreservesAutomationAndRemoteRestore() {
        val settings = SettingsStore(
            automatorMode = AutomatorMode.Automation.value,
            allowAuxiliaryA11y = true,
            enableAutomator = true,
        )
        // System disable does not pass through shutdownAuxiliary() or shutdown(true).
        val reason = A11yStopReason.resolve(
            replacement = false,
            auxiliary = AppScopePolicy.auxiliaryA11y(settings, settings.useA11y),
            temporary = false,
        )
        assertEquals(A11yStopReason.Auxiliary, reason)
        assertFalse(reason.disablesAutomator(ownsServiceState = true))
        val lease = RemoteAutomationLease()
        lease.acquire("computer-task", 0, wasRunning = true)
        if (reason.disablesAutomator(true)) lease.cancelRestore()
        lease.release("computer-task")
        assertTrue(lease.shouldRestore)
    }

    @Test
    fun helperPromotedToPrimaryStillDisablesAutomationOnSystemStop() {
        val settings = SettingsStore(
            automatorMode = AutomatorMode.A11y.value,
            allowAuxiliaryA11y = true,
        )
        val reason = A11yStopReason.resolve(
            replacement = false,
            auxiliary = AppScopePolicy.auxiliaryA11y(settings, settings.useA11y),
            temporary = false,
        )
        assertTrue(reason.disablesAutomator(true))
        assertFalse(reason.disablesAutomator(false))
    }

    @Test
    fun replacementAndTemporaryStopsPreserveEnabledState() {
        val replacement = A11yStopReason.resolve(true, true, true)
        assertEquals(A11yStopReason.Replacement, replacement)
        assertFalse(replacement.disablesAutomator(true))
        val temporary = A11yStopReason.resolve(false, false, true)
        assertEquals(A11yStopReason.Temporary, temporary)
        assertFalse(temporary.disablesAutomator(true))
        // Explicit helper shutdown retains its identity after the option has been turned off.
        assertEquals(A11yStopReason.Auxiliary, A11yStopReason.resolve(false, true, true))
    }
}
