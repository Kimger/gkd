package li.gkd.app.ui.home

actual fun previewActionToast(text: String, system: Boolean, woodfish: Boolean, woodfishSize: Int) =
    li.gkd.app.util.AndroidToastUtils.previewAction(text, system, woodfish, woodfishSize)
