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
        assertEquals("[\"handshake\"]", encoded)
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
}