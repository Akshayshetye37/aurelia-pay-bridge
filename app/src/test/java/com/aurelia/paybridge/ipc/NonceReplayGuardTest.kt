package com.aurelia.paybridge.ipc

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NonceReplayGuardTest {
    @Test fun acceptsNonceOnlyOnceWithinTtl() {
        val guard = NonceReplayGuard(ttlMillis = 1000, maxEntries = 10)
        assertTrue(guard.accept("nonce-123456789012345", 10))
        assertFalse(guard.accept("nonce-123456789012345", 11))
    }

    @Test fun acceptsNonceAgainAfterExpiry() {
        val guard = NonceReplayGuard(ttlMillis = 1000, maxEntries = 10)
        assertTrue(guard.accept("nonce-123456789012345", 10))
        assertTrue(guard.accept("nonce-123456789012345", 1010))
    }

    @Test fun rejectsNewNonceWhenCacheIsFullWithoutEvictingLiveEntries() {
        val guard = NonceReplayGuard(ttlMillis = 10_000, maxEntries = 2)
        assertTrue(guard.accept("nonce-aaaaaaaaaaaa", 1))
        assertTrue(guard.accept("nonce-bbbbbbbbbbbb", 2))

        assertFalse(guard.accept("nonce-cccccccccccc", 3))
        assertFalse(guard.accept("nonce-aaaaaaaaaaaa", 4))
        assertFalse(guard.accept("nonce-bbbbbbbbbbbb", 5))
    }

    @Test fun acceptsNewNonceAfterOldEntryExpiresAtCapacity() {
        val guard = NonceReplayGuard(ttlMillis = 10, maxEntries = 2)
        assertTrue(guard.accept("nonce-aaaaaaaaaaaa", 1))
        assertTrue(guard.accept("nonce-bbbbbbbbbbbb", 2))

        assertTrue(guard.accept("nonce-cccccccccccc", 11))
        assertFalse(guard.accept("nonce-aaaaaaaaaaaa", 12))
    }

    @Test fun rejectsBlankNonceAndInvalidClock() {
        val guard = NonceReplayGuard()
        assertFalse(guard.accept("", 1))
        assertFalse(guard.accept("nonce-123456789012345", -1))
    }
}
