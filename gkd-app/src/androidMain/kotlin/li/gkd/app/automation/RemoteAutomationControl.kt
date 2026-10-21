package li.gkd.app.automation

import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import li.gkd.app.appScope
import li.gkd.app.priv.AutomationService
import li.gkd.app.priv.privilegeContextFlow
import li.gkd.app.priv.uiAutomationFlow
import li.gkd.app.service.currentAppBlocked
import li.gkd.app.service.currentAppUseA11y
import li.gkd.app.settings.SettingsRepository
import li.gkd.app.util.LogUtils
import li.gkd.app.util.launchLogged

object RemoteAutomationControl {
    private val lease = RemoteAutomationLease()
    private val mutex = Mutex()
    private var maintenanceJob: Job? = null

    val isPaused: Boolean get() = lease.paused

    fun cancelRestore() = lease.cancelRestore()

    suspend fun setEnabled(enabled: Boolean) = mutex.withLock {
        SettingsRepository.updateSettings { it.copy(enableRemoteAutomationControl = enabled) }
        if (!enabled) {
            lease.releaseAll()
            startMaintenance()
        }
    }

    /** The receiver's manifest permission restricts this protocol to privileged ADB callers. */
    suspend fun command(command: String, token: String?): String = mutex.withLock {
        check(SettingsRepository.settings.value.enableRemoteAutomationControl) { "feature_disabled" }
        lease.expire(SystemClock.elapsedRealtime())
        if (command != "status") {
            require(token != null && token.matches(Regex("[A-Za-z0-9_-]{16,128}"))) { "invalid_token" }
        }
        when (command) {
            "pause" -> {
                check(SettingsRepository.settings.value.useAutomation) { "automation_mode_required" }
                check(SettingsRepository.settings.value.enableAutomator) { "automation_disabled" }
                check(lease.paused || lease.shouldRestore || uiAutomationFlow.value != null) {
                    "automation_not_running"
                }
                check(lease.acquire(requireNotNull(token), SystemClock.elapsedRealtime(), uiAutomationFlow.value != null)) {
                    "another_task_owns_pause"
                }
                startMaintenance()
                AutomationService.pauseForRemoteControl()
                check(!AutomationService.isOtherUiAutomationRunning()) { "automation_still_occupied" }
                LogUtils.d("Remote automation paused")
            }
            "heartbeat" -> check(lease.renew(requireNotNull(token), SystemClock.elapsedRealtime())) { "lease_expired_or_wrong_token" }
            "release" -> {
                check(lease.release(requireNotNull(token))) { "lease_expired_or_wrong_token" }
                LogUtils.d("Remote automation released")
                tryRestore()
            }
            "status" -> Unit
            else -> error("unknown_command")
        }
        startMaintenance()
        when {
            lease.paused -> "paused"
            lease.shouldRestore -> "waiting_for_automation"
            uiAutomationFlow.value != null -> "running"
            else -> "idle"
        }
    }

    private fun startMaintenance() {
        if (maintenanceJob?.isActive == true) return
        if (!lease.paused && !lease.shouldRestore) return
        maintenanceJob = appScope.launchLogged(Dispatchers.IO) {
            while (isActive) {
                delay(1_000L)
                val finished = mutex.withLock {
                    if (lease.expire(SystemClock.elapsedRealtime())) {
                        LogUtils.d("Remote automation heartbeat expired")
                    }
                    tryRestore()
                    (!lease.paused && !lease.shouldRestore).also {
                        if (it) maintenanceJob = null
                    }
                }
                if (finished) break
            }
        }
    }

    private fun tryRestore() {
        if (lease.paused || !lease.shouldRestore) return
        val settings = SettingsRepository.settings.value
        if (!settings.enableAutomator || !settings.useAutomation) {
            lease.cancelRestore()
            return
        }
        if (privilegeContextFlow.value == null || currentAppBlocked || currentAppUseA11y) return
        try {
            if (AutomationService.isOtherUiAutomationRunning()) return
            AutomationService.tryConnect(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Keep the recovery request pending when the privileged service is temporarily unavailable.
            LogUtils.d("Remote automation restore failed; will retry", e)
            return
        }
        if (uiAutomationFlow.value != null) {
            lease.cancelRestore()
            LogUtils.d("Remote automation restored")
        }
    }
}
