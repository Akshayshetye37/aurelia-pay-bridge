package com.aurelia.paybridge.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.cert.X509Certificate

/**
 * Device-local, attested signing identity for future Bridge evidence envelopes.
 *
 * This component signs bytes; it does not decide whether a payment succeeded.
 * The appliance must verify the returned attestation chain and provider evidence
 * independently before authorizing any accounting action.
 */
object HardwareEvidenceKey {
    private const val KEY_ALIAS = "aurelia.paybridge.evidence.ec.v1"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    private const val MAX_PAYLOAD_BYTES = 64 * 1024
    private const val MIN_CHALLENGE_BYTES = 16
    private const val MAX_CHALLENGE_BYTES = 128

    data class AttestedIdentity(
        val keyId: String,
        val algorithm: String,
        val publicKeyBase64: String,
        /** X.509 certificate encodings, leaf first, Base64 without line wrapping. */
        val attestationChainBase64: List<String>,
        /** STRONGBOX, TRUSTED_ENVIRONMENT, or legacy hardware-backed classification. */
        val securityLevel: String
    )

    data class SignedPayload(
        val keyId: String,
        val algorithm: String,
        val signatureBase64: String
    )

    /**
     * Creates a fresh enrollment key bound to the appliance's one-time challenge.
     *
     * Calling this method intentionally replaces the previous key. The caller
     * must complete owner-authorized enrollment and retain the returned chain.
     * StrongBox is preferred on API 28+; TEE is the only permitted fallback.
     */
    @Synchronized
    fun createAttestedIdentity(serverChallenge: ByteArray): AttestedIdentity {
        require(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            "Key attestation requires Android 7.0 (API 24) or later"
        }
        require(serverChallenge.size in MIN_CHALLENGE_BYTES..MAX_CHALLENGE_BYTES) {
            "Server attestation challenge must be 16–128 bytes"
        }

        val keyStore = loadKeyStore()
        // New one-time server challenge means a new key; never reuse an old
        // certificate chain that was attested against a different challenge.
        if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)

        val privateKey = generateKey(serverChallenge, preferStrongBox = true)
        val info = getKeyInfo(privateKey)
        check(info.isInsideSecureHardware) {
            "Android Keystore did not create a hardware-backed evidence key"
        }

        val chain = keyStore.getCertificateChain(KEY_ALIAS)
            ?.map { certificate ->
                val x509 = certificate as? X509Certificate
                    ?: throw IllegalStateException("Unexpected non-X.509 attestation certificate")
                Base64.encodeToString(x509.encoded, Base64.NO_WRAP)
            }
            ?: throw IllegalStateException("Android Keystore returned no attestation certificate chain")
        check(chain.isNotEmpty()) { "Attestation certificate chain is empty" }

        val certificate = keyStore.getCertificate(KEY_ALIAS)
            ?: throw IllegalStateException("Evidence key certificate is missing")
        val publicKeyBytes = certificate.publicKey.encoded

        return AttestedIdentity(
            keyId = sha256Hex(publicKeyBytes),
            algorithm = SIGNATURE_ALGORITHM,
            publicKeyBase64 = Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP),
            attestationChainBase64 = chain,
            securityLevel = securityLevel(info)
        )
    }

    /**
     * Signs exact bytes from the future versioned canonical evidence encoder.
     * This method refuses to create an un-attested key as a side effect.
     */
    @Synchronized
    fun signCanonicalPayload(payload: ByteArray): SignedPayload {
        require(payload.isNotEmpty()) { "Evidence payload must not be empty" }
        require(payload.size <= MAX_PAYLOAD_BYTES) { "Evidence payload exceeds 64 KiB" }

        val keyStore = loadKeyStore()
        val privateKey = keyStore.getKey(KEY_ALIAS, null) as? java.security.PrivateKey
            ?: throw IllegalStateException("No attested evidence key; device enrollment is required")
        val info = getKeyInfo(privateKey)
        check(info.isInsideSecureHardware) {
            "Evidence private key is not hardware-backed"
        }

        val certificate = keyStore.getCertificate(KEY_ALIAS)
            ?: throw IllegalStateException("Evidence key certificate is missing")
        val keyId = sha256Hex(certificate.publicKey.encoded)

        val signer = Signature.getInstance(SIGNATURE_ALGORITHM)
        signer.initSign(privateKey)
        signer.update(payload)
        return SignedPayload(
            keyId = keyId,
            algorithm = SIGNATURE_ALGORITHM,
            signatureBase64 = Base64.encodeToString(signer.sign(), Base64.NO_WRAP)
        )
    }

    private fun generateKey(challenge: ByteArray, preferStrongBox: Boolean): java.security.PrivateKey {
        if (preferStrongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                return generateKeyPair(challenge, strongBox = true).private
            } catch (_: StrongBoxUnavailableException) {
                // StrongBox is optional. Retry only key generation in TEE;
                // this is not a payment retry and cannot trigger money movement.
            }
        }
        return generateKeyPair(challenge, strongBox = false).private
    }

    private fun generateKeyPair(challenge: ByteArray, strongBox: Boolean): java.security.KeyPair {
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE)
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setAttestationChallenge(challenge)
            .setUserAuthenticationRequired(false)

        if (strongBox) builder.setIsStrongBoxBacked(true)
        generator.initialize(builder.build())
        return generator.generateKeyPair()
    }

    private fun getKeyInfo(privateKey: java.security.PrivateKey): KeyInfo {
        val factory = KeyFactory.getInstance(privateKey.algorithm, KEYSTORE)
        return factory.getKeySpec(privateKey, KeyInfo::class.java)
    }

    private fun securityLevel(info: KeyInfo): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return when (info.securityLevel) {
                KeyProperties.SECURITY_LEVEL_STRONGBOX -> "STRONGBOX"
                KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> "TRUSTED_ENVIRONMENT"
                else -> "UNSUPPORTED_SECURITY_LEVEL"
            }
        }
        return if (info.isInsideSecureHardware) {
            "HARDWARE_BACKED_LEVEL_UNSPECIFIED"
        } else {
            "SOFTWARE"
        }
    }

    private fun loadKeyStore(): KeyStore =
        KeyStore.getInstance(KEYSTORE).apply { load(null) }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
            "%02x".format(java.util.Locale.ROOT, it)
        }
}
