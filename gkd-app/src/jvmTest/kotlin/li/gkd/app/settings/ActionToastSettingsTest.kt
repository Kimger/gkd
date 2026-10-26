package li.gkd.app.settings

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import li.gkd.app.subscription.SubscriptionJson.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ActionToastSettingsTest {
    // A saved user size must not be discarded when settings are decoded and rewritten.
    @Test
    fun selectedWoodfishSizeSurvivesSettingsRoundTrip() {
        val settings = json.decodeFromString<SettingsStore>(
            """{"useCatWoodfishToast":true,"catWoodfishSize":65}"""
        )
        val saved = json.parseToJsonElement(json.encodeToString(settings)).jsonObject
        assertEquals("65", saved["catWoodfishSize"]?.jsonPrimitive?.content)
    }

    @Test
    fun legacySettingsKeepExistingSystemAndCustomStyles() {
        for (system in listOf(false, true)) {
            val settings = json.decodeFromString<SettingsStore>("""{"useSystemToast":$system}""")
            assertFalse(settings.useCatWoodfishToast)
            assertEquals(system, settings.useSystemToast)
            assertEquals(40, settings.catWoodfishSize)
        }
    }

    // Protects the persisted opt-in field: ignoring it would silently disable the selected style.
    @Test
    fun selectedWoodfishStyleSurvivesSettingsRoundTrip() {
        val settings = json.decodeFromString<SettingsStore>(
            """{"useCatWoodfishToast":true,"useSystemToast":false,"actionToast":"My text"}"""
        )
        val saved = json.parseToJsonElement(json.encodeToString(settings)).jsonObject
        assertEquals("true", saved["useCatWoodfishToast"]?.jsonPrimitive?.content)
        assertEquals("My text", saved["actionToast"]?.jsonPrimitive?.content)
    }
}
