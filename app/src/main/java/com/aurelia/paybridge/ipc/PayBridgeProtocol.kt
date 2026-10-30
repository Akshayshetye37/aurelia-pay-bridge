package com.aurelia.paybridge.ipc

object PayBridgeProtocol {
    const val ACTION_BIND = "com.aurelia.paybridge.BIND"
    const val PMS_PACKAGE = "com.aurelia.pms"
    const val BIND_PERMISSION = "com.aurelia.paybridge.permission.BIND_PAY_BRIDGE"
    const val PROTOCOL_VERSION = 1

    const val MSG_HANDSHAKE_REQUEST = 100
    const val MSG_HANDSHAKE_RESPONSE = 101

    const val KEY_PROTOCOL_MIN = "protocolMin"
    const val KEY_PROTOCOL_MAX = "protocolMax"
    const val KEY_PROTOCOL_VERSION = "protocolVersion"
    const val KEY_NONCE = "nonce"
    const val KEY_PACKAGE_NAME = "packageName"
    const val KEY_CAPABILITIES_JSON = "capabilitiesJson"

    // This release implements authentication/handshake only. Never advertise
    // payment, refund, provider-management or reconciliation operations early.
    val CAPABILITIES = arrayOf("handshake")
}