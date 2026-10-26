package li.gkd.app.ui.home

import li.gkd.app.DesktopRuntime

actual fun previewActionToast(text: String, system: Boolean, woodfish: Boolean, woodfishSize: Int) {
    val runtime = DesktopRuntime.requireCurrent()
    if (woodfish) {
        runtime.toast.dismiss(runtime.toast.revision)
        runtime.catWoodfishToast.show(woodfishSize)
    } else {
        runtime.catWoodfishToast.request?.let(runtime.catWoodfishToast::dismiss)
        runtime.toast.show(text)
    }
}
