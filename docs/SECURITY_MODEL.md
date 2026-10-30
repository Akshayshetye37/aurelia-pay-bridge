# Security model and implementation boundaries

## Trust boundaries

1. PMS authenticates the installed Pay Bridge package and shared Android signing identity over explicit Android Messenger IPC.
2. Pay Bridge accepts IPC only from the configured PMS package and verifies that the Binder caller UID owns that package and that PMS and Pay Bridge have the same Android signing identity. The Android signature-level service permission is an additional binding gate; it is not a replacement for the service-side caller checks.
3. Pay Bridge launches provider SDKs only with short-lived checkout data issued by the Aurelia backend.
4. The backend creates provider orders, stores merchant credentials, validates webhook signatures, independently fetches provider transaction state, enforces idempotency, and emits verified payment events.
5. PMS remains responsible for folio accounting. An SDK callback is never sufficient evidence to post a folio payment.

## Never ship in this APK

- Razorpay key secret
- Cashfree client secret
- PayU salt
- PhonePe client secret
- Webhook signing secrets
- Backend administrative tokens
- Production private keys or signing keystores

A provider key ID/public identifier may be delivered at runtime where the provider requires it, but no secret may be embedded in resources, BuildConfig, assets, native libraries, or source control.

## IPC baseline

The service is exported for the explicit PMS component and protected by the signature permission `com.aurelia.paybridge.permission.BIND_PAY_BRIDGE`. Both APKs must be signed by the same approved certificate for Android's signature permission to be granted. The service independently verifies the Binder caller UID, PMS package ownership and shared signer before responding.

Protocol v1 advertises only `handshake`. The service processes messages on a dedicated HandlerThread and rejects reused nonces within a bounded, ten-minute, process-lifetime replay cache. Expired records are pruned; when all cache slots contain unexpired records, new handshakes are rejected rather than evicting a live replay record. A process restart clears the in-memory cache, so this is not durable replay prevention. PMS must generate an unpredictable nonce, validate the exact outstanding nonce echo, and enforce its handshake timeout. No payment, refund, provider-management, or reconciliation messages are implemented or advertised.

## Payment result handling

Provider SDK callbacks mean only that the user journey returned. The app must send the provider reference to the backend's authenticated verification endpoint, or ask the backend to resolve the order. Do not send an authoritative success event to PMS until the backend reports a verified captured/paid state.

## Operational controls still required

- Certificate pin/rotation strategy coordinated with PMS.
- Durable payment request IDs and idempotency across retries.
- Backend authentication and device enrollment.
- Audit logging without PAN, CVV, OTP, access tokens, or secrets.
- Release signing via protected CI secrets.
- Provider-specific security/compliance review before production.
