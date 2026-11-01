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

    override fun onBind(intent: Intent?): IBinder? {
        // Do not expose the Messenger endpoint to callers using an unrelated action.
        if (intent?.action != PayBridgeProtocol.ACTION_BIND) {
            Log.w(TAG, "Rejected bind request with an unexpected action")
            return null
        }
        return messenger.binder
    }

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
        val handshake = readHandshakeRequest(message.data) ?: return false

        // A nonce is single-use for the lifetime of this service process. Expired
        // entries are pruned. At capacity, new handshakes fail closed rather than
        // evicting an unexpired replay record. Process death still clears this cache.
        if (!nonceReplayGuard.accept(handshake.nonce, SystemClock.elapsedRealtime())) {
            Log.w(TAG, "Rejected duplicate nonce or handshake while replay cache is saturated")
            return false
        }

        val response = Message.obtain(null, PayBridgeProtocol.MSG_HANDSHAKE_RESPONSE).apply {
            data = Bundle().apply {
                putInt(
                    PayBridgeProtocol.KEY_PROTOCOL_VERSION,
                    PayBridgeProtocol.PROTOCOL_VERSION
                )
                putString(PayBridgeProtocol.KEY_NONCE, handshake.nonce)
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

    /**
     * Accept only the four protocol-v1 handshake fields. In particular, reject
     * unexpected Parcelable payloads/extension keys instead of silently ignoring
     * them. Bundle decoding is caller-controlled and therefore guarded.
     */
    private fun readHandshakeRequest(bundle: Bundle?): HandshakeRequest? {
        return try {
            val request = bundle ?: return null
            val keys = request.keySet()
            if (keys != HandshakeRequestValidator.ALLOWED_KEYS) return null

            val minVersion = request.getInt(PayBridgeProtocol.KEY_PROTOCOL_MIN, -1)
            val maxVersion = request.getInt(PayBridgeProtocol.KEY_PROTOCOL_MAX, -1)
            val nonce = request.getString(PayBridgeProtocol.KEY_NONCE) ?: return null
            val requestedPackage =
                request.getString(PayBridgeProtocol.KEY_PACKAGE_NAME) ?: return null

            if (!HandshakeRequestValidator.isValid(
                    minVersion,
                    maxVersion,
                    nonce,
                    requestedPackage,
                    keys
                )
            ) {
                return null
            }
            HandshakeRequest(nonce)
        } catch (exception: RuntimeException) {
            // Malformed or unparcellable caller data must not terminate the IPC thread.
            Log.w(TAG, "Rejected malformed PMS handshake bundle", exception)
            null
        }
    }

    private fun isAuthorizedPmsCaller(uid: Int): Boolean {
        if (uid < 0) return false
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

    private data class HandshakeRequest(val nonce: String)

    companion object {
        private const val TAG = "AureliaPayBridge"
    }
}
