package com.aurelia.paybridge.security

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.KeyFactory
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.spec.X509EncodedKeySpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HardwareEvidenceKeyTest {
    @Test
    fun createsChallengeBoundHardwareKeyAndVerifiableSignature() {
        val challenge = ByteArray(32) { index -> (index + 1).toByte() }
        val identity = HardwareEvidenceKey.createAttestedIdentity(challenge)

        assertEquals("SHA256withECDSA", identity.algorithm)
        assertTrue(identity.keyId.isNotBlank())
        assertTrue(identity.attestationChainBase64.isNotEmpty())
        assertTrue(
            identity.securityLevel == "STRONGBOX" ||
                identity.securityLevel == "TRUSTED_ENVIRONMENT" ||
                identity.securityLevel == "HARDWARE_BACKED_LEVEL_UNSPECIFIED"
        )

        val leafBytes = Base64.decode(identity.attestationChainBase64.first(), Base64.NO_WRAP)
        val leaf = CertificateFactory.getInstance("X.509")
            .generateCertificate(leafBytes.inputStream())
        assertEquals(
            identity.publicKeyBase64,
            Base64.encodeToString(leaf.publicKey.encoded, Base64.NO_WRAP)
        )

        val payload = "aurelia.paybridge.evidence.self-test.v1".toByteArray(Charsets.UTF_8)
        val signed = HardwareEvidenceKey.signCanonicalPayload(payload)
        assertEquals(identity.keyId, signed.keyId)

        val publicKey = KeyFactory.getInstance("EC")
            .generatePublic(
                X509EncodedKeySpec(Base64.decode(identity.publicKeyBase64, Base64.NO_WRAP))
            )
        val verifier = Signature.getInstance("SHA256withECDSA")
        verifier.initVerify(publicKey)
        verifier.update(payload)
        assertTrue(verifier.verify(Base64.decode(signed.signatureBase64, Base64.NO_WRAP)))
    }
}
