package li.gkd.app.automation

import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import li.gkd.app.app
import li.gkd.app.appScope
import li.gkd.app.permission.PermissionStates
import li.gkd.app.priv.privilegeContextFlow
import li.gkd.app.priv.uiAutomationFlow
import li.gkd.app.resources.*
import li.gkd.app.service.A11yService
import li.gkd.app.service.currentAppBlocked
import li.gkd.app.service.currentAppUseA11y
import li.gkd.app.service.currentAppUseAuxiliaryA11y
import li.gkd.app.settings.SettingsRepository
import li.gkd.app.settings.AppScopePolicy
import li.gkd.app.ui.share.launchUi
import li.gkd.app.util.ThreadUtils
import li.gkd.app.util.ToastUtils
import org.jetbrains.compose.resources.getString

/** Explicit setting/automation-start events only; no polling or automatic rule fallback. */
object AuxiliaryA11yController {
    private var startJob: Job? = null

    suspend fun setEnabled(enabled: Boolean) {
        withContext(Dispatchers.Main.immediate) {
            SettingsRepository.updateSettings { it.copy(allowAuxiliaryA11y = enabled) }
            if (enabled) onAutomationStarted() else {
                startJob?.cancel()
                if (SettingsRepository.settings.value.useAutomation && !currentAppUseA11y) {
                    A11yService.instance?.shutdownAuxiliary()
                }
            }
        }
        SettingsRepository.awaitPersistence()
    }

    val shouldRun: Boolean
        get() = AppScopePolicy.auxiliaryA11yShouldRun(
            SettingsRepository.settings.value, currentAppUseA11y,
            uiAutomationFlow.value != null, currentAppBlocked, RemoteAutomationControl.isPaused,
        )

    fun onAutomationStarted() = ThreadUtils.runMainOrPost {
        if (!shouldRun || A11yService.isRunning.value || startJob?.isActive == true) return@runMainOrPost
        startJob = appScope.launchUi {
            if (privilegeContextFlow.value == null || !PermissionStates.writeSecureSettings.updateAndGet()) {
                ToastUtils.show(getString(Res.string.auxiliary_a11y_permission_required))
                return@launchUi
            }
            withContext(Dispatchers.IO) {
                if (!shouldRun) return@withContext
                val services = app.getSecureA11yServices()
                // An enabled but unbound service needs the same re-enable used by the existing tile.
                if (services.remove(A11yService.a11yCn)) {
                    app.putSecureA11yServices(services)
                    delay(1000)
                }
                if (!shouldRun) return@withContext
                // Re-read after waiting so other services enabled by the user are preserved.
                val current = app.getSecureA11yServices()
                current.add(A11yService.a11yCn)
                app.putSecureInt(Settings.Secure.ACCESSIBILITY_ENABLED, 1)
                app.putSecureA11yServices(current)
            }
            delay(2000)
            if (shouldRun && !A11yService.isRunning.value) {
                ToastUtils.show(getString(Res.string.auxiliary_a11y_start_failed))
            }
        }
    }

    fun onAutomationStopped() = ThreadUtils.runMainOrPost {
        if (uiAutomationFlow.value != null) return@runMainOrPost
        startJob?.cancel()
        if (currentAppUseAuxiliaryA11y) A11yService.instance?.shutdownAuxiliary()
    }
}
