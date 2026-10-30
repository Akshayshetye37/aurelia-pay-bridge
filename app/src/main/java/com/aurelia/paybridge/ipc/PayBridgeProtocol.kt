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

    // Configure from the approved PMS release certificate before deployment.
    // Empty by default: the service rejects every caller until configured.
    const val EXPECTED_PMS_CERT_SHA256 = ""

    val CAPABILITIES = arrayOf("handshake")
}
