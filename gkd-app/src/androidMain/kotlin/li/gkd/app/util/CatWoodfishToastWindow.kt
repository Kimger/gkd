package li.gkd.app.util

import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import li.gkd.app.app
import li.gkd.app.ui.component.GkCatWoodfishToast

/** Owns just this short-lived overlay composition, never an Activity or a page ViewModel. */
class CatWoodfishToastWindow(
    private val windowManager: WindowManager,
    private val size: Int,
    private val onFinished: () -> Unit,
) : SavedStateRegistryOwner {
    override val lifecycle = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this).apply {
        performAttach()
        performRestore(null)
    }
    override val savedStateRegistry get() = controller.savedStateRegistry
    private val view = ComposeView(app).apply {
        setViewTreeLifecycleOwner(this@CatWoodfishToastWindow)
        setViewTreeSavedStateRegistryOwner(this@CatWoodfishToastWindow)
        setContent {
            GkCatWoodfishToast(bodySize = size, onFinished = { post { onFinished() } })
        }
    }

    fun show(params: WindowManager.LayoutParams) {
        lifecycle.currentState = Lifecycle.State.RESUMED
        try {
            windowManager.addView(view, params)
        } catch (e: RuntimeException) {
            view.disposeComposition()
            lifecycle.currentState = Lifecycle.State.DESTROYED
            throw e
        }
    }

    fun close() {
        view.disposeComposition()
        lifecycle.currentState = Lifecycle.State.DESTROYED
        if (view.isAttachedToWindow) windowManager.removeViewImmediate(view)
    }
}
