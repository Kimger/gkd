package li.gkd.app.rule

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaskForegroundPolicyTest {
    @Test
    fun rejectsLauncherReportWhileActiveWindowStillBelongsToCurrentApp() {
        assertFalse(acceptTaskForegroundApp("launcher", "bank", "launcher") { "bank" })
    }

    @Test
    fun acceptsActualHomeAndUnavailableOrDifferentWindows() {
        assertTrue(acceptTaskForegroundApp("launcher", "bank", "launcher") { "launcher" })
        assertTrue(acceptTaskForegroundApp("launcher", "bank", "launcher") { null })
        assertTrue(acceptTaskForegroundApp("launcher", "bank", "launcher") { "other.app" })
    }

    @Test
    fun ordinaryTaskChangesDoNotReadAccessibilityWindows() {
        val unexpectedRead: () -> String? = { error("Unexpected window read") }
        assertTrue(acceptTaskForegroundApp("bank", "launcher", "launcher", unexpectedRead))
        assertTrue(acceptTaskForegroundApp("other.app", "bank", "launcher", unexpectedRead))
        assertTrue(acceptTaskForegroundApp("launcher", "launcher", "launcher", unexpectedRead))
        assertTrue(acceptTaskForegroundApp("launcher", "", "launcher", unexpectedRead))
    }
}
