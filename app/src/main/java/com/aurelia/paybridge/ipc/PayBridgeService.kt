package com.aurelia.paybridge.ipc

import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.SystemClock
import android.util.Log
import org.json.JSONArray

/**
 * PMS-facing IPC endpoint. Android's signature permission gates binding; this
 * service independently checks the Binder caller UID, package and shared signer.
 * Handshake processing runs on a dedicated thread, never the main application thread.
 */
class PayBridgeService : Service() {
    private lateinit var ipcThread: HandlerThread
    private lateinit var messenger: Messenger
    private val nonceReplayGuard = NonceReplayGuard()

    override fun onCreate() {
        super.onCreate()
        ipcThread = HandlerThread("AureliaPayBridgeIpc").apply { start() }
        messenger = Messenger(Handler(ipcThread.looper, ::handleMessage))
    }

    override fun onBind(intent: Intent?): IBinder = messenger.binder

    override fun onDestroy() {
        if (::ipcThread.isInitialized) {
            ipcThread.quitSafely()
        }
        super.onDestroy()
    }

    private fun handleMessage(message: Message): Boolean {
        if (message.what != PayBridgeProtocol.MSG_HANDSHAKE_REQUEST) return false

        // Check caller identity before processing any caller-controlled payload.
        if (!isAuthorizedPmsCaller(message.sendingUid)) {
            Log.w(TAG, "Rejected unauthorised IPC caller UID ${message.sendingUid}")
            return false
        }
        val replyTo = message.replyTo ?: return false

        val request = message.data ?: Bundle.EMPTY
        val minVersion = request.getInt(PayBridgeProtocol.KEY_PROTOCOL_MIN, -1)
        val maxVersion = request.getInt(PayBridgeProtocol.KEY_PROTOCOL_MAX, -1)
        val nonce = request.getString(PayBridgeProtocol.KEY_NONCE) ?: return false
        val requestedPackage =
            request.getString(PayBridgeProtocol.KEY_PACKAGE_NAME) ?: return false

        if (requestedPackage != PayBridgeProtocol.PMS_PACKAGE ||
            minVersion > PayBridgeProtocol.PROTOCOL_VERSION ||
            maxVersion < PayBridgeProtocol.PROTOCOL_VERSION ||
            minVersion < 1 || maxVersion < minVersion ||
            nonce.length !in 16..256 ||
            !NONCE_PATTERN.matches(nonce)
        ) {
            return false
        }

        // A nonce is single-use for the lifetime of this service process. Expired
        // entries are pruned. At capacity, new handshakes fail closed rather than
        // evicting an unexpired replay record. Process death still clears this cache.
        if (!nonceReplayGuard.accept(nonce, SystemClock.elapsedRealtime())) {
            Log.w(TAG, "Rejected duplicate nonce or handshake while replay cache is saturated")
            return false
        }

        val response = Message.obtain(null, PayBridgeProtocol.MSG_HANDSHAKE_RESPONSE).apply {
            data = Bundle().apply {
                putInt(
                    PayBridgeProtocol.KEY_PROTOCOL_VERSION,
                    PayBridgeProtocol.PROTOCOL_VERSION
                )
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
        } catch (exception: Exception) {
            Log.w(TAG, "Could not reply to PMS handshake", exception)
            false
        }
    }

    private fun isAuthorizedPmsCaller(uid: Int): Boolean {
        if (uid == PackageManager.INVALID_UID) return false
        val packages = packageManager.getPackagesForUid(uid) ?: return false
        if (PayBridgeProtocol.PMS_PACKAGE !in packages) return false
        return try {
            packageManager.checkSignatures(
                PayBridgeProtocol.PMS_PACKAGE,
                packageName
            ) == PackageManager.SIGNATURE_MATCH
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val TAG = "AureliaPayBridge"
        private val NONCE_PATTERN = Regex("[A-Za-z0-9._:-]+")
    }
}
