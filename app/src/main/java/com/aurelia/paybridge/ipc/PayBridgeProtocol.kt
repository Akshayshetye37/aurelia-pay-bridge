package com.aurelia.paybridge.ipc

/**
 * Wire identifiers shared with PMS android/templates/PayBridgeProtocol.java.
 *
 * Protocol v1 implements handshake only. The operation identifiers below are
 * reserved contract constants, not executable IPC. Do not advertise their
 * capabilities or accept these messages until both APKs and the trusted backend
 * implement and test the same contract.
 */
object PayBridgeProtocol {
    const val ACTION_BIND = "com.aurelia.paybridge.BIND"
    const val PMS_PACKAGE = "com.aurelia.pms"
    const val BIND_PERMISSION = "com.aurelia.paybridge.permission.BIND_PAY_BRIDGE"
    const val PROTOCOL_VERSION = 1

    const val MSG_HANDSHAKE_REQUEST = 100
    const val MSG_HANDSHAKE_RESPONSE = 101

    // Reserved operation IDs. The service deliberately rejects these today.
    const val MSG_CREATE_PAYMENT = 200
    const val MSG_CREATE_PAYMENT_RESULT = 201
    const val MSG_GET_PAYMENT_STATUS = 202
    const val MSG_PAYMENT_STATUS_RESULT = 203
    const val MSG_REQUEST_REFUND = 300
    const val MSG_REQUEST_REFUND_RESULT = 301
    const val MSG_GET_REFUND_STATUS = 302
    const val MSG_REFUND_STATUS_RESULT = 303
    const val MSG_RECONCILE = 400
    const val MSG_RECONCILE_RESULT = 401
    const val MSG_PAYMENT_EVENT = 500
    const val MSG_EVENT_ACK = 501

    const val KEY_PROTOCOL_MIN = "protocolMin"
    const val KEY_PROTOCOL_MAX = "protocolMax"
    const val KEY_PROTOCOL_VERSION = "protocolVersion"
    const val KEY_NONCE = "nonce"
    const val KEY_PACKAGE_NAME = "packageName"
    const val KEY_CAPABILITIES_JSON = "capabilitiesJson"

    // Reserved payload keys mirrored from the PMS-side contract.
    const val KEY_REQUEST_ID = "requestId"
    const val KEY_TRANSACTION_ID = "transactionId"
    const val KEY_BOOKING_ID = "bookingId"
    const val KEY_FOLIO_ID = "folioId"
    const val KEY_AMOUNT_PAISE = "amountPaise"
    const val KEY_CURRENCY = "currency"
    const val KEY_PROVIDER_ID = "providerId"
    const val KEY_MERCHANT_REFERENCE = "merchantReference"
    const val KEY_STATUS = "status"
    const val KEY_EVIDENCE_ID = "evidenceId"

    const val CAP_HANDSHAKE = "handshake"
    const val CAP_PAYMENT_CREATE = "payment.create"
    const val CAP_PAYMENT_STATUS = "payment.status"
    const val CAP_REFUND_CREATE = "refund.create"
    const val CAP_REFUND_STATUS = "refund.status"
    const val CAP_RECONCILIATION = "payment.reconciliation"
    const val CAP_PAYMENT_EVENTS = "payment.events"

    const val MAX_REQUEST_ID_LENGTH = 128
    const val MAX_REFERENCE_LENGTH = 128
    const val MAX_MESSAGE_BYTES = 64 * 1024
    const val CURRENCY_INR = "INR"

    // Only capabilities backed by executable, tested service handlers belong here.
    val CAPABILITIES = arrayOf(CAP_HANDSHAKE)
}
