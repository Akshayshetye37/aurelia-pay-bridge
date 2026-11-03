package com.aurelia.paybridge.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.ECGenParameterSpec

/**
 * Device-local signing identity for future Bridge evidence envelopes.
 *
 * This class deliberately does not decide whether a payment succeeded. It only
 * signs caller-supplied, already-canonicalized bytes. The future protocol must
 * define canonical serialization, server enrollment, key revocation/rotation,
 * and provider-evidence verification before these signatures can be trusted.
 *
 * Software-backed keys are rejected. Android Keystore keeps the private key
 * non-exportable and KeyInfo confirms TEE/StrongBox-backed storage.
 */
object HardwareEvidenceKey {
    private const val KEY_ALIAS = "aurelia.paybridge.evidence.ec.v1"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    private const val MAX_PAYLOAD_BYTES = 64 * 1024

    data class PublicIdentity(
        val keyId: String,
        val algorithm: String,
        val publicKeyBase64: String
    )

    data class SignedPayload(
        val keyId: String,
        val algorithm: String,
        val signatureBase64: String
    )

    @Synchronized
    fun getOrCreateIdentity(): PublicIdentity {
        val keyStore = loadKeyStore()
        var privateKey = keyStore.getKey(KEY_ALIAS, null) as? java.security.PrivateKey

        if (privateKey != null && !isHardwareBacked(privateKey)) {
            // Never silently continue with a software key under the production alias.
            keyStore.deleteEntry(KEY_ALIAS)
            privateKey = null
        }

        if (privateKey == null) {
            val generator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                KEYSTORE
            )
            generator.initialize(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                )
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setUserAuthenticationRequired(false)
                    .build()
            )
            privateKey = generator.generateKeyPair().private
            check(isHardwareBacked(privateKey)) {
                "Android Keystore did not create a hardware-backed evidence key"
            }
        }

        val certificate = keyStore.getCertificate(KEY_ALIAS)
            ?: throw IllegalStateException("Evidence key certificate is missing")
        val publicKeyBytes = certificate.publicKey.encoded
        return PublicIdentity(
            keyId = sha256Hex(publicKeyBytes),
            algorithm = SIGNATURE_ALGORITHM,
            publicKeyBase64 = Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP)
        )
    }

    /**
     * Signs exact bytes. Callers must use the future versioned canonical evidence
     * encoder; do not sign ad-hoc JSON or treat this signature as provider proof.
     */
    @Synchronized
    fun signCanonicalPayload(payload: ByteArray): SignedPayload {
        require(payload.isNotEmpty()) { "Evidence payload must not be empty" }
        require(payload.size <= MAX_PAYLOAD_BYTES) { "Evidence payload exceeds 64 KiB" }

        val identity = getOrCreateIdentity()
        val keyStore = loadKeyStore()
        val privateKey = keyStore.getKey(KEY_ALIAS, null) as? java.security.PrivateKey
            ?: throw IllegalStateException("Hardware evidence private key is unavailable")
        check(isHardwareBacked(privateKey)) {
            "Evidence private key is not hardware-backed"
        }

        val signer = Signature.getInstance(SIGNATURE_ALGORITHM)
        signer.initSign(privateKey)
        signer.update(payload)
        return SignedPayload(
            keyId = identity.keyId,
            algorithm = SIGNATURE_ALGORITHM,
            signatureBase64 = Base64.encodeToString(signer.sign(), Base64.NO_WRAP)
        )
    }

    private fun isHardwareBacked(privateKey: java.security.PrivateKey): Boolean {
        val factory = KeyFactory.getInstance(privateKey.algorithm, KEYSTORE)
        val info = factory.getKeySpec(privateKey, KeyInfo::class.java)
        return info.isInsideSecureHardware
    }

    private fun loadKeyStore(): KeyStore =
        KeyStore.getInstance(KEYSTORE).apply { load(null) }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
            "%02x".format(java.util.Locale.ROOT, it)
        }
}
