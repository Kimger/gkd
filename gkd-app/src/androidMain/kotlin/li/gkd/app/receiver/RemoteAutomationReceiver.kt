package li.gkd.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import li.gkd.app.appScope
import li.gkd.app.automation.RemoteAutomationControl
import li.gkd.app.util.LogUtils
import li.gkd.app.util.launchLogged

class RemoteAutomationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "li.songe.gkd.REMOTE_AUTOMATION") return
        val pending = goAsync()
        appScope.launchLogged(Dispatchers.IO) {
            try {
                val state = RemoteAutomationControl.command(
                    intent.getStringExtra("command") ?: "",
                    intent.getStringExtra("token"),
                )
                pending.setResult(0, state, null)
            } catch (e: CancellationException) {
                pending.setResult(1, "cancelled", null)
                throw e
            } catch (e: Exception) {
                LogUtils.d("Remote automation command failed", e)
                pending.setResult(1, e.message ?: "command_failed", null)
            } finally {
                pending.finish()
            }
        }
    }
}
