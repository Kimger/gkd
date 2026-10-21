package li.gkd.app.automation

/** Monotonic times are supplied by the host; leases never change the user's saved settings. */
class RemoteAutomationLease(private val timeoutMillis: Long = 30_000L) {
    private var owner: String? = null
    private var deadline = 0L
    private var restore = false

    val paused: Boolean get() = synchronized(this) { owner != null }
    val shouldRestore: Boolean get() = synchronized(this) { restore }

    fun acquire(token: String, now: Long, wasRunning: Boolean): Boolean = synchronized(this) {
        if (owner != null && owner != token) return false
        if (owner == null) restore = restore || wasRunning
        owner = token
        deadline = now + timeoutMillis
        true
    }

    fun renew(token: String, now: Long): Boolean = synchronized(this) {
        if (owner != token || now >= deadline) return false
        deadline = now + timeoutMillis
        true
    }

    fun release(token: String): Boolean = synchronized(this) {
        if (owner != token) return false
        owner = null
        true
    }

    fun expire(now: Long): Boolean = synchronized(this) {
        if (owner == null || now < deadline) return false
        owner = null
        true
    }

    fun releaseAll() = synchronized(this) { owner = null }

    fun cancelRestore() = synchronized(this) { restore = false }
}
