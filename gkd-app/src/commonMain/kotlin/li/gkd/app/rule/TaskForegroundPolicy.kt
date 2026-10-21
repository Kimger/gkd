package li.gkd.app.rule

/** Only a transition to the launcher needs confirmation against the active window. */
fun acceptTaskForegroundApp(
    reportedAppId: String,
    currentAppId: String,
    launcherAppId: String,
    activeWindowAppId: () -> String?,
): Boolean {
    if (reportedAppId != launcherAppId || currentAppId == launcherAppId || currentAppId.isEmpty()) {
        return true
    }
    return activeWindowAppId() != currentAppId
}
