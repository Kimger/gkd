package li.gkd.app.settings

object AppScopePolicy {
    fun auxiliaryA11y(settings: SettingsStore, primaryUsesA11y: Boolean): Boolean =
        settings.allowAuxiliaryA11y && settings.useAutomation && !primaryUsesA11y

    fun effectiveMode(settings: SettingsStore, primaryUsesA11y: Boolean, latestMode: Int): Int =
        if (auxiliaryA11y(settings, primaryUsesA11y)) AutomatorMode.Automation.value else latestMode

    fun auxiliaryA11yShouldRun(
        settings: SettingsStore,
        primaryUsesA11y: Boolean,
        automationRunning: Boolean,
        blocked: Boolean,
        paused: Boolean,
    ): Boolean = auxiliaryA11y(settings, primaryUsesA11y) && settings.enableAutomator &&
        automationRunning && !blocked && !paused

    fun blockedA11y(
        settings: SettingsStore,
        blockedMatch: Set<String>,
        blockedA11y: Set<String>
    ): Set<String> =
        if (settings.blockA11yAppListFollowMatch) blockedMatch else blockedA11y

    fun a11yScope(settings: SettingsStore, scope: Set<String>): Set<String> =
        if (settings.useAutomation) scope else emptySet()

    fun blocksMatch(
        settings: SettingsStore,
        blockedMatch: Set<String>,
        blockedA11y: Set<String>,
        appId: String
    ): Boolean =
        appId in blockedMatch || (settings.enableBlockA11yAppList && appId in blockedA11y(
            settings,
            blockedMatch,
            blockedA11y
        ))
}
