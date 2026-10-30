package com.aurelia.paybridge.ipc

/**
 * Bounded, process-lifetime replay protection for PMS handshake nonces.
 * Call with Android elapsedRealtime(), not wall-clock time.
 */
internal class NonceReplayGuard(
    private val ttlMillis: Long = 10 * 60 * 1000L,
    private val maxEntries: Int = 4096
) {
    private val accepted = LinkedHashMap<String, Long>()

    init {
        require(ttlMillis > 0) { "ttlMillis must be positive" }
        require(maxEntries > 0) { "maxEntries must be positive" }
    }

    @Synchronized
    fun accept(nonce: String, nowElapsedMillis: Long): Boolean {
        if (nonce.isBlank() || nowElapsedMillis < 0) return false

        val iterator = accepted.entries.iterator()
        while (iterator.hasNext()) {
            val age = nowElapsedMillis - iterator.next().value
            if (age >= ttlMillis || age < 0) iterator.remove()
        }

        if (accepted.containsKey(nonce)) return false
        while (accepted.size >= maxEntries) {
            val oldest = accepted.entries.iterator()
            if (!oldest.hasNext()) break
            oldest.next()
            oldest.remove()
        }
        accepted[nonce] = nowElapsedMillis
        return true
    }
}
