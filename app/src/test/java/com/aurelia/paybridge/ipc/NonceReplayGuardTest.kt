package com.aurelia.paybridge.ipc

import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
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

    @Test fun purgesExpiredEntriesBeforeRejectingAtCapacity() {
        val guard = NonceReplayGuard(ttlMillis = 10, maxEntries = 2)
        assertTrue(guard.accept("nonce-aaaaaaaaaaaa", 1))
        assertTrue(guard.accept("nonce-bbbbbbbbbbbb", 2))

        // A is expired; B is still live. The expired slot must be reclaimed.
        assertTrue(guard.accept("nonce-cccccccccccc", 11))
        assertFalse(guard.accept("nonce-cccccccccccc", 11))

        // B expires at t=12 and may then be accepted as a fresh nonce.
        assertTrue(guard.accept("nonce-bbbbbbbbbbbb", 12))
    }

    @Test fun concurrentRequestsWithSameNonceAcceptExactlyOnce() {
        val guard = NonceReplayGuard(ttlMillis = 10_000, maxEntries = 128)
        val workers = 32
        val barrier = CyclicBarrier(workers)
        val pool = Executors.newFixedThreadPool(workers)

        try {
            val results = (0 until workers).map {
                pool.submit(Callable {
                    barrier.await(5, TimeUnit.SECONDS)
                    guard.accept("nonce-concurrent-123456", 100)
                })
            }
            val acceptedCount = results.count { it.get(10, TimeUnit.SECONDS) }
            assertEquals(1, acceptedCount)
        } finally {
            pool.shutdownNow()
        }
    }

    @Test fun rejectsBlankNonceAndInvalidClock() {
        val guard = NonceReplayGuard()
        assertFalse(guard.accept("", 1))
        assertFalse(guard.accept("nonce-123456789012345", -1))
    }
}
