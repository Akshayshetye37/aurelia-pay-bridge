package com.aurelia.paybridge.ipc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class PayBridgeProtocolTest {
    @Test fun protocolVersionIsOne() {
        assertEquals(1, PayBridgeProtocol.PROTOCOL_VERSION)
    }

    @Test fun onlyImplementedCapabilityIsAdvertised() {
        assertArrayEquals(arrayOf("handshake"), PayBridgeProtocol.CAPABILITIES)
    }
}
