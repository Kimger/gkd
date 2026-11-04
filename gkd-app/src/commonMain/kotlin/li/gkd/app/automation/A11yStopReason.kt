package li.gkd.app.automation

/** A helper disappearing must not change the primary automation's enabled state. */
enum class A11yStopReason {
    Replacement,
    Auxiliary,
    Temporary,
    Primary;

    fun disablesAutomator(ownsServiceState: Boolean): Boolean =
        this == Primary && ownsServiceState

    companion object {
        fun resolve(
            replacement: Boolean,
            auxiliary: Boolean,
            temporary: Boolean,
        ): A11yStopReason = when {
            replacement -> Replacement
            auxiliary -> Auxiliary
            temporary -> Temporary
            else -> Primary
        }
    }
}
