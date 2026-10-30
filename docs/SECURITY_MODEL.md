# Security model and implementation boundaries

## Trust boundaries

1. PMS authenticates the installed Pay Bridge package and signing identity over explicit Android Messenger IPC.
2. Pay Bridge accepts IPC only from the configured PMS signing-certificate SHA-256 digest. The build constant is deliberately blank by default, so the exported service rejects callers until the correct PMS certificate digest is supplied.
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

The service is exported for the PMS explicit component and protected by the signature permission `com.aurelia.pms.permission.BIND_PAY_BRIDGE`. Both APKs must be signed by the same approved certificate for Android's signature permission to be granted. The service additionally checks the PMS caller certificate digest. Confirm the exact permission declaration and certificate fingerprint with the PMS repository before integration.

Only the `handshake` capability is advertised. No payment, refund, provider-management, or reconciliation messages are implemented in this commit.

## Payment result handling

Provider SDK callbacks mean only that the user journey returned. The app must send the provider reference to the backend's authenticated verification endpoint, or ask the backend to resolve the order. Do not send an authoritative success event to PMS until the backend reports a verified captured/paid state.

## Operational controls still required

- Certificate pin/rotation strategy coordinated with PMS.
- Nonce replay protection and session expiry in the final IPC protocol.
- Durable request IDs and idempotency across retries.
- Backend authentication and device enrollment.
- Audit logging without PAN, CVV, OTP, access tokens, or secrets.
- Release signing via protected CI secrets.
- Provider-specific security/compliance review before production.
