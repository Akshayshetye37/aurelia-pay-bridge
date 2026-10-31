package com.aurelia.paybridge.ipc

import org.json.JSONArray
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PayBridgeProtocolTest {
    @Test fun protocolVersionIsOne() {
        assertEquals(1, PayBridgeProtocol.PROTOCOL_VERSION)
    }

    @Test fun pmsAndBridgePermissionContractMatches() {
        assertEquals("com.aurelia.pms", PayBridgeProtocol.PMS_PACKAGE)
        assertEquals(
            "com.aurelia.paybridge.permission.BIND_PAY_BRIDGE",
            PayBridgeProtocol.BIND_PERMISSION
        )
    }

    @Test fun onlyImplementedCapabilityIsAdvertised() {
        assertArrayEquals(arrayOf("handshake"), PayBridgeProtocol.CAPABILITIES)
    }

    @Test fun capabilitiesUsePmsJsonWireFormat() {
        val encoded = JSONArray(PayBridgeProtocol.CAPABILITIES.toList()).toString()
        assertEquals("[\\"handshake\\"]", encoded)
        assertEquals("handshake", JSONArray(encoded).getString(0))
    }

    @Test fun protocolRangeMustContainVersionOne() {
        fun compatible(min: Int, max: Int) =
            min <= PayBridgeProtocol.PROTOCOL_VERSION &&
                max >= PayBridgeProtocol.PROTOCOL_VERSION &&
                min >= 1 && max >= min

        assertTrue(compatible(1, 1))
        assertFalse(compatible(2, 2))
        assertFalse(compatible(1, 0))
    }

    @Test fun reservedOperationIdsMatchPmsContract() {
        assertEquals(200, PayBridgeProtocol.MSG_CREATE_PAYMENT)
        assertEquals(201, PayBridgeProtocol.MSG_CREATE_PAYMENT_RESULT)
        assertEquals(202, PayBridgeProtocol.MSG_GET_PAYMENT_STATUS)
        assertEquals(203, PayBridgeProtocol.MSG_PAYMENT_STATUS_RESULT)
        assertEquals(300, PayBridgeProtocol.MSG_REQUEST_REFUND)
        assertEquals(301, PayBridgeProtocol.MSG_REQUEST_REFUND_RESULT)
        assertEquals(302, PayBridgeProtocol.MSG_GET_REFUND_STATUS)
        assertEquals(303, PayBridgeProtocol.MSG_REFUND_STATUS_RESULT)
        assertEquals(400, PayBridgeProtocol.MSG_RECONCILE)
        assertEquals(401, PayBridgeProtocol.MSG_RECONCILE_RESULT)
        assertEquals(500, PayBridgeProtocol.MSG_PAYMENT_EVENT)
        assertEquals(501, PayBridgeProtocol.MSG_EVENT_ACK)
    }

    @Test fun reservedCapabilitiesAreNotAdvertised() {
        assertFalse(PayBridgeProtocol.CAPABILITIES.contains(PayBridgeProtocol.CAP_PAYMENT_CREATE))
        assertFalse(PayBridgeProtocol.CAPABILITIES.contains(PayBridgeProtocol.CAP_PAYMENT_STATUS))
        assertFalse(PayBridgeProtocol.CAPABILITIES.contains(PayBridgeProtocol.CAP_REFUND_CREATE))
        assertFalse(PayBridgeProtocol.CAPABILITIES.contains(PayBridgeProtocol.CAP_REFUND_STATUS))
        assertFalse(PayBridgeProtocol.CAPABILITIES.contains(PayBridgeProtocol.CAP_RECONCILIATION))
        assertFalse(PayBridgeProtocol.CAPABILITIES.contains(PayBridgeProtocol.CAP_PAYMENT_EVENTS))
    }

    @Test fun reservedPayloadLimitsMatchPmsContract() {
        assertEquals(128, PayBridgeProtocol.MAX_REQUEST_ID_LENGTH)
        assertEquals(128, PayBridgeProtocol.MAX_REFERENCE_LENGTH)
        assertEquals(64 * 1024, PayBridgeProtocol.MAX_MESSAGE_BYTES)
        assertEquals("INR", PayBridgeProtocol.CURRENCY_INR)
    }
}
