package com.aurelia.paybridge.ipc

/**
 * Bounded, process-lifetime replay protection for PMS handshake nonces.
 *
 * Uses Android elapsedRealtime(), not wall-clock time. When capacity is reached,
 * new nonces are rejected until an entry expires; unexpired replay records are
 * never evicted to make room for new requests.
 *
 * This is defense in depth, not durable replay prevention: process death clears
 * this in-memory cache. The PMS must also validate its outstanding random nonce
 * and enforce a short handshake timeout.
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
            val acceptedAt = iterator.next().value
            // elapsedRealtime() is monotonic. If a caller supplies a regressed
            // timestamp, fail closed and preserve all replay records.
            if (nowElapsedMillis < acceptedAt) return false
            if (nowElapsedMillis - acceptedAt >= ttlMillis) iterator.remove()
        }

        if (accepted.containsKey(nonce)) return false
        if (accepted.size >= maxEntries) return false

        accepted[nonce] = nowElapsedMillis
        return true
    }
}
