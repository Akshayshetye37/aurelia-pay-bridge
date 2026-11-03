package com.aurelia.paybridge.security

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HardwareEvidenceKeyTest {
    @Test
    fun createsStableHardwareBackedIdentityAndVerifiableSignature() {
        val first = HardwareEvidenceKey.getOrCreateIdentity()
        val second = HardwareEvidenceKey.getOrCreateIdentity()

        assertEquals(first.keyId, second.keyId)
        assertEquals(first.publicKeyBase64, second.publicKeyBase64)
        assertEquals("SHA256withECDSA", first.algorithm)

        val payload = """{"protocolVersion":1,"purpose":"keystore-self-test"}"""
            .toByteArray(Charsets.UTF_8)
        val signed = HardwareEvidenceKey.signCanonicalPayload(payload)
        assertEquals(first.keyId, signed.keyId)

        val publicKeyBytes = Base64.decode(first.publicKeyBase64, Base64.NO_WRAP)
        val publicKey = KeyFactory.getInstance("EC")
            .generatePublic(X509EncodedKeySpec(publicKeyBytes))
        val verifier = Signature.getInstance("SHA256withECDSA")
        verifier.initVerify(publicKey)
        verifier.update(payload)
        assertTrue(verifier.verify(Base64.decode(signed.signatureBase64, Base64.NO_WRAP)))
    }
}
