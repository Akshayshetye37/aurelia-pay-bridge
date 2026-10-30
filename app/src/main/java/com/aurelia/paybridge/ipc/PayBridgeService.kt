package com.aurelia.paybridge.ipc

import android.app.Service
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import org.json.JSONArray

/**
 * PMS-facing IPC endpoint. Android's signature permission gates binding; this
 * service independently checks the caller UID, package name and signer before
 * returning any protocol data.
 */
class PayBridgeService : Service() {
    private val messenger by lazy {
        Messenger(Handler(Looper.getMainLooper(), ::handleMessage))
    }

    override fun onBind(intent: android.content.Intent?): IBinder = messenger.binder

    private fun handleMessage(message: Message): Boolean {
        if (message.what != PayBridgeProtocol.MSG_HANDSHAKE_REQUEST) return false
        val replyTo = message.replyTo ?: return false

        if (!isAuthorizedPmsCaller(message.sendingUid)) {
            Log.w(TAG, "Rejected unauthorised IPC caller UID ${message.sendingUid}")
            return false
        }

        val request = message.data ?: Bundle.EMPTY
        val minVersion = request.getInt(PayBridgeProtocol.KEY_PROTOCOL_MIN, -1)
        val maxVersion = request.getInt(PayBridgeProtocol.KEY_PROTOCOL_MAX, -1)
        val nonce = request.getString(PayBridgeProtocol.KEY_NONCE) ?: return false
        val requestedPackage = request.getString("packageName") ?: return false

        if (requestedPackage != PayBridgeProtocol.PMS_PACKAGE ||
            minVersion > PayBridgeProtocol.PROTOCOL_VERSION ||
            maxVersion < PayBridgeProtocol.PROTOCOL_VERSION ||
            minVersion < 1 || maxVersion < minVersion ||
            nonce.length !in 16..256 ||
            !nonce.matches(Regex("[A-Za-z0-9._:-]+"))) {
            return false
        }

        val response = Message.obtain(null, PayBridgeProtocol.MSG_HANDSHAKE_RESPONSE).apply {
            data = Bundle().apply {
                putInt(PayBridgeProtocol.KEY_PROTOCOL_VERSION, PayBridgeProtocol.PROTOCOL_VERSION)
                putString(PayBridgeProtocol.KEY_NONCE, nonce)
                putString(PayBridgeProtocol.KEY_PACKAGE_NAME, packageName)
                putString(
                    PayBridgeProtocol.KEY_CAPABILITIES_JSON,
                    JSONArray(PayBridgeProtocol.CAPABILITIES.toList()).toString()
                )
            }
        }

        return try {
            replyTo.send(response)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Could not reply to PMS handshake", e)
            false
        }
    }

    private fun isAuthorizedPmsCaller(uid: Int): Boolean {
        val packages = packageManager.getPackagesForUid(uid) ?: return false
        if (PayBridgeProtocol.PMS_PACKAGE !in packages) return false
        return try {
            packageManager.checkSignatures(PayBridgeProtocol.PMS_PACKAGE, packageName) ==
                PackageManager.SIGNATURE_MATCH
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val TAG = "AureliaPayBridge"
    }
}