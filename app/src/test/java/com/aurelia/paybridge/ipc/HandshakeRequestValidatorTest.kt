package com.aurelia.paybridge.ipc

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandshakeRequestValidatorTest {
    private val keys = HandshakeRequestValidator.ALLOWED_KEYS

    private fun valid(
        min: Int = 1,
        max: Int = 1,
        nonce: String = "0123456789abcdef",
        packageName: String = PayBridgeProtocol.PMS_PACKAGE,
        suppliedKeys: Set<String> = keys
    ) = HandshakeRequestValidator.isValid(min, max, nonce, packageName, suppliedKeys)

    @Test fun acceptsExactProtocolV1HandshakeEnvelope() {
        assertTrue(valid())
    }

    @Test fun rejectsUnsupportedOrInvertedProtocolRanges() {
        assertFalse(valid(min = 0))
        assertFalse(valid(min = 2, max = 2))
        assertFalse(valid(min = 1, max = 0))
    }

    @Test fun rejectsUnexpectedOrMissingFields() {
        assertFalse(valid(suppliedKeys = keys + "unexpected"))
        assertFalse(valid(suppliedKeys = keys - PayBridgeProtocol.KEY_NONCE))
    }

    @Test fun rejectsWrongCallerPackage() {
        assertFalse(valid(packageName = "com.example.attacker"))
    }

    @Test fun rejectsShortLongOrMalformedNonces() {
        assertFalse(valid(nonce = "short"))
        assertFalse(valid(nonce = "a".repeat(257)))
        assertFalse(valid(nonce = "0123456789abcde!"))
        assertFalse(valid(nonce = ""))
    }
}
