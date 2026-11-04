package li.gkd.app.priv

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import li.gkd.app.appScope
import li.gkd.app.app
import li.gkd.app.priv.shizuku.GkdShizukuExternalStartProvider
import li.gkd.app.util.launchLogged
import priv.kit.core.Privilege
import priv.kit.ui.PrivilegeUi
import rikka.shizuku.Shizuku

private const val RECOVERY_COOLDOWN_MILLIS = 10 * 60 * 1000L
private val recoveryMutex = Mutex()
private var recoveryAllowedAt = 0L

fun requestPrivilegeRecovery() {
    appScope.launchLogged(Dispatchers.IO) { recoverPrivilege() }
}

fun listenForShizukuPrivilegeRecovery() {
    Shizuku.addBinderReceivedListenerSticky {
        appScope.launchLogged(Dispatchers.IO) {
            recoverPrivilege(shizukuReady = true)
        }
    }
}

suspend fun recoverPrivilege(shizukuReady: Boolean = false) = recoveryMutex.withLock {
    Privilege.serverState.value?.let {
        return@withLock it
    }
    if (!PrivilegeUi.desiredEnabled.value) {
        return@withLock null
    }
    if (shizukuReady) {
        if (!GkdShizukuExternalStartProvider.snapshot(app).canStart) return@withLock null
    } else if (SystemClock.elapsedRealtime() < recoveryAllowedAt) {
        return@withLock null
    }
    PrivilegeUi.startSilently(gkdPrivilegeUiConfig).also { serverInfo ->
        if (serverInfo == null && PrivilegeUi.desiredEnabled.value) {
            recoveryAllowedAt = SystemClock.elapsedRealtime() + RECOVERY_COOLDOWN_MILLIS
        }
    }
}

suspend fun clearPrivilegeRecoveryCooldown() {
    recoveryMutex.withLock { recoveryAllowedAt = 0L }
}
