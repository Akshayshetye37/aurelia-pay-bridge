package com.aurelia.paybridge.ipc

object PayBridgeProtocol {
    const val ACTION_BIND = "com.aurelia.paybridge.BIND"
    const val PROTOCOL_VERSION = 1

    const val MSG_HANDSHAKE_REQUEST = 100
    const val MSG_HANDSHAKE_RESPONSE = 101

    const val KEY_PROTOCOL_VERSION = "protocolVersion"
    const val KEY_NONCE = "nonce"
    const val KEY_PACKAGE_NAME = "packageName"
    const val KEY_CERT_SHA256 = "certSha256"
    const val KEY_CAPABILITIES = "capabilities"

    // Only handshake is advertised in this foundation build. Do not advertise
    // payment/refund/provider-management until those operations are implemented.
    val CAPABILITIES = arrayOf("handshake")
}
