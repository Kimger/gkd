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

/** A real application window may correct a stale launcher report without waiting for its grace period. */
fun canCorrectLauncherReport(
    currentAppId: String,
    eventAppId: String,
    launcherAppId: String,
    activeWindowAppId: () -> String?,
): Boolean {
    if (currentAppId != launcherAppId || eventAppId == launcherAppId || eventAppId.isEmpty()) {
        return false
    }
    return activeWindowAppId() == eventAppId
}
