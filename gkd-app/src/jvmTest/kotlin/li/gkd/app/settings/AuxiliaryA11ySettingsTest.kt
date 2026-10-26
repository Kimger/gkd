package li.gkd.app.settings

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import li.gkd.app.subscription.SubscriptionJson.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuxiliaryA11ySettingsTest {
    @Test
    fun lateBindingIsRejectedAfterStopPauseBlockOrModeChange() {
        val settings = SettingsStore(
            allowAuxiliaryA11y = true, enableAutomator = true, automatorMode = AutomatorMode.Automation.value,
        )
        assertTrue(AppScopePolicy.auxiliaryA11yShouldRun(settings, false, true, false, false))
        assertFalse(AppScopePolicy.auxiliaryA11yShouldRun(settings, false, false, false, false))
        assertFalse(AppScopePolicy.auxiliaryA11yShouldRun(settings, false, true, false, true))
        assertFalse(AppScopePolicy.auxiliaryA11yShouldRun(settings, false, true, true, false))
        assertFalse(AppScopePolicy.auxiliaryA11yShouldRun(settings, true, true, false, false))
        assertFalse(AppScopePolicy.auxiliaryA11yShouldRun(settings.copy(enableAutomator = false), false, true, false, false))
        assertFalse(AppScopePolicy.auxiliaryA11yShouldRun(settings.copy(allowAuxiliaryA11y = false), false, true, false, false))
    }

    @Test
    fun legacySettingsKeepMutuallyExclusiveServices() {
        val value = json.decodeFromString<SettingsStore>("""{"automatorMode":2}""")
        assertFalse(value.allowAuxiliaryA11y)
        assertFalse(AppScopePolicy.auxiliaryA11y(value, false))
    }

    @Test
    fun helperNeverBecomesTheExecutorEvenIfItConnectedLast() {
        val settings = SettingsStore(allowAuxiliaryA11y = true, automatorMode = AutomatorMode.Automation.value)
        assertTrue(AppScopePolicy.auxiliaryA11y(settings, false))
        assertEquals(
            AutomatorMode.Automation.value,
            AppScopePolicy.effectiveMode(settings, false, AutomatorMode.A11y.value),
        )
    }

    @Test
    fun ordinaryModeAndExplicitA11yScopeKeepExistingSelection() {
        val settings = SettingsStore(allowAuxiliaryA11y = true, automatorMode = AutomatorMode.Automation.value)
        assertFalse(AppScopePolicy.auxiliaryA11y(settings, true))
        assertEquals(AutomatorMode.A11y.value, AppScopePolicy.effectiveMode(settings, true, AutomatorMode.A11y.value))
        assertFalse(AppScopePolicy.auxiliaryA11y(settings.copy(automatorMode = AutomatorMode.A11y.value), true))
        assertEquals(
            AutomatorMode.A11y.value,
            AppScopePolicy.effectiveMode(settings.copy(allowAuxiliaryA11y = false), false, AutomatorMode.A11y.value),
        )
    }

    @Test
    fun auxiliaryOptInSurvivesSettingsRewrite() {
        val value = json.decodeFromString<SettingsStore>("""{"allowAuxiliaryA11y":true}""")
        val saved = json.parseToJsonElement(json.encodeToString(value)).jsonObject
        assertEquals("true", saved["allowAuxiliaryA11y"]?.jsonPrimitive?.content)
    }
}
