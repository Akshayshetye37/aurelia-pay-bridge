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
import java.security.MessageDigest

class PayBridgeService : Service() {
    private val messenger by lazy {
        Messenger(Handler(Looper.getMainLooper(), ::handleMessage))
    }

    override fun onBind(intent: android.content.Intent?): IBinder = messenger.binder

    private fun handleMessage(message: Message): Boolean {
        if (message.what != PayBridgeProtocol.MSG_HANDSHAKE_REQUEST) return false
        val replyTo = message.replyTo ?: return false
        if (!isAuthorizedPmsCaller(message.sendingUid)) {
            Log.w(TAG, "Rejected IPC caller UID \${message.sendingUid}")
            return false
        }

        val request = message.data ?: Bundle.EMPTY
        val nonce = request.getString(PayBridgeProtocol.KEY_NONCE) ?: return false
        val version = request.getInt(PayBridgeProtocol.KEY_PROTOCOL_VERSION, -1)
        if (version != PayBridgeProtocol.PROTOCOL_VERSION || nonce.length !in 16..256) return false

        val response = Message.obtain(null, PayBridgeProtocol.MSG_HANDSHAKE_RESPONSE).apply {
            data = Bundle().apply {
                putInt(PayBridgeProtocol.KEY_PROTOCOL_VERSION, PayBridgeProtocol.PROTOCOL_VERSION)
                putString(PayBridgeProtocol.KEY_NONCE, nonce)
                putString(PayBridgeProtocol.KEY_PACKAGE_NAME, packageName)
                putString(PayBridgeProtocol.KEY_CERT_SHA256, ownSigningCertSha256())
                putStringArray(PayBridgeProtocol.KEY_CAPABILITIES, PayBridgeProtocol.CAPABILITIES)
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
        val expected = PayBridgeProtocol.EXPECTED_PMS_CERT_SHA256
            .replace(":", "")
            .uppercase()
        if (!expected.matches(Regex("[A-F0-9]{64}"))) return false

        val packages = packageManager.getPackagesForUid(uid) ?: return false
        return packages.any { packageName ->
            runCatching { signingCertSha256(packageName) == expected }.getOrDefault(false)
        }
    }

    private fun ownSigningCertSha256(): String = signingCertSha256(packageName)

    @Suppress("DEPRECATION")
    private fun signingCertSha256(targetPackage: String): String {
        val info = if (android.os.Build.VERSION.SDK_INT >= 28) {
            packageManager.getPackageInfo(targetPackage, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            packageManager.getPackageInfo(targetPackage, PackageManager.GET_SIGNATURES)
        }
        val signatures = if (android.os.Build.VERSION.SDK_INT >= 28) {
            info.signingInfo?.apkContentsSigners
        } else {
            info.signatures
        } ?: throw SecurityException("No signing certificate")
        val digest = MessageDigest.getInstance("SHA-256").digest(signatures.first().toByteArray())
        return digest.joinToString("") { "%02X".format(it) }
    }

    companion object {
        private const val TAG = "AureliaPayBridge"
    }
}
