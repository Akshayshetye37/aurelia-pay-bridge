package com.aurelia.paybridge.ipc

/**
 * Pure validation for the protocol-v1 handshake envelope.
 *
 * The allowlist is intentionally exact: adding fields requires a protocol
 * revision and matching PMS-side contract/test changes.
 */
internal object HandshakeRequestValidator {
    val ALLOWED_KEYS = setOf(
        PayBridgeProtocol.KEY_PROTOCOL_MIN,
        PayBridgeProtocol.KEY_PROTOCOL_MAX,
        PayBridgeProtocol.KEY_NONCE,
        PayBridgeProtocol.KEY_PACKAGE_NAME
    )

    private val noncePattern = Regex("[A-Za-z0-9._:-]+")

    fun isValid(
        minVersion: Int,
        maxVersion: Int,
        nonce: String,
        requestedPackage: String,
        keys: Set<String>
    ): Boolean {
        return keys == ALLOWED_KEYS &&
            requestedPackage == PayBridgeProtocol.PMS_PACKAGE &&
            minVersion >= PayBridgeProtocol.PROTOCOL_MIN &&
            minVersion <= PayBridgeProtocol.PROTOCOL_VERSION &&
            maxVersion >= PayBridgeProtocol.PROTOCOL_VERSION &&
            maxVersion <= PayBridgeProtocol.PROTOCOL_MAX &&
            maxVersion >= minVersion &&
            nonce.length in 16..256 &&
            noncePattern.matches(nonce)
    }
}
