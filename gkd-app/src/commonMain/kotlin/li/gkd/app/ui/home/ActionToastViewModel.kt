package li.gkd.app.ui.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import li.gkd.app.settings.SettingsRepository
import li.gkd.app.ui.page.EditorActionsState
import li.gkd.app.ui.state.BaseViewModel

class ActionToastViewModel : BaseViewModel() {
    val editorActions = EditorActionsState(scope)

    private val initial = SettingsRepository.settings.value

    val enabled: StateFlow<Boolean>
        field = MutableStateFlow(initial.toastWhenClick)

    fun setEnabled(value: Boolean) {
        enabled.value = value
    }

    val systemStyle: StateFlow<Boolean>
        field = MutableStateFlow(initial.useSystemToast)

    fun setSystemStyle(value: Boolean) {
        systemStyle.value = value
        woodfishStyle.value = false
    }

    val woodfishStyle: StateFlow<Boolean>
        field = MutableStateFlow(initial.useCatWoodfishToast)

    fun setWoodfishStyle() {
        woodfishStyle.value = true
        systemStyle.value = false
    }

    val woodfishSize: StateFlow<Int>
        field = MutableStateFlow(initial.catWoodfishSize.coerceIn(20, 80))

    fun setWoodfishSize(value: Int) {
        woodfishSize.value = value.coerceIn(20, 80)
    }

    val text: StateFlow<String>
        field = MutableStateFlow(initial.actionToast)

    fun setText(value: String) {
        text.value = value
    }

    fun hasChanges() =
        enabled.value != initial.toastWhenClick ||
            systemStyle.value != initial.useSystemToast ||
            woodfishStyle.value != initial.useCatWoodfishToast ||
            woodfishSize.value != initial.catWoodfishSize ||
            text.value != initial.actionToast

    suspend fun save() = SettingsRepository.saveActionToast(
        enabled.value, systemStyle.value, text.value, woodfishStyle.value, woodfishSize.value,
    )
}
