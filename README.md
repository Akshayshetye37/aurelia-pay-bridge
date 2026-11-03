# Aurelia Pay Bridge


## Current delivery status and remaining work — 2026-11-03

**Status: development only. No production payment execution, device enrollment, or live collection is enabled.**

### Current source location

- PMS: [Akshayshetye37/Aurelia-apk](https://github.com/Akshayshetye37/Aurelia-apk), branch `feature/multi-provider-payment-backend`.
- Pay Bridge: [Akshayshetye37/aurelia-pay-bridge](https://github.com/Akshayshetye37/aurelia-pay-bridge), branch `feature/hardware-backed-evidence-key`.

### Current PR #2 status

[PR #2 — hardware-backed evidence signing key](https://github.com/Akshayshetye37/aurelia-pay-bridge/pull/2) remains **draft/open**.

- CI run [41646944152](https://github.com/Akshayshetye37/aurelia-pay-bridge/actions/runs/41646944152): JVM unit tests passed; debug APK and Android instrumentation APK compilation passed.
- CI did not execute the instrumentation test.
- StrongBox/TEE generation, returned attestation chain, leaf/public-key match and software-key rejection still require supported physical-device testing.
- Bridge exports the public key and attestation chain. It does not validate the chain, enroll the device, or activate the key.
- Current protocol v1 advertises only `handshake`. No payment-execution or refund messages are implemented or enabled.

### Remaining implementation — dependency order

#### P0 — Bridge key lifecycle and physical validation

- Stage newly generated keys separately; do not delete or replace the currently active key before the new key has passed appliance verification and owner-authorized activation.
- Add safe promotion/rollback and explicit key-version handling for rotation.
- Fail closed on unsupported/unknown hardware security levels.
- Execute instrumentation tests on physical StrongBox and TEE-capable devices; record actual device/API level, security level, attestation-chain result and test outcome.
- Test release-signed PMS/Bridge pairing on-device using the approved matching signing certificate.

#### P0 — Appliance-side enrollment (tracked in [Issue #3](https://github.com/Akshayshetye37/aurelia-pay-bridge/issues/3))

- Owner-authenticated challenge issuance, bound to property and intended device.
- Appliance validation of Google's current Android Key Attestation trust roots, certificate revocation, challenge freshness/one-time use, package and signer identity, security levels, verified boot/device lock, patch policy and leaf-key binding.
- Atomic registration as pending activation; explicit owner activation; visible Bridge activation state; revocation and safe rotation.
- Do not accept evidence or device commands from unregistered, pending or revoked devices.

#### P1 — Only after P0 acceptance

- Define canonical signed evidence format, strict request/job/property/folio/amount/currency binding, server nonce, timestamp/expiry and replay protection.
- Implement authenticated IPC contracts only after enrollment is live and validated. Keep payment-execution messages out of protocol v1 until the separate execution contract is approved.
- Add provider SDK adapters only with server-created checkout sessions and independent server-side provider verification.
- Add cancellation/status handling without blind retries; ambiguous provider outcomes must remain ambiguous until reconciled.

#### P2 — Release acceptance

- Physical-device IPC and key-attestation matrix.
- Provider sandbox checkout, webhook signature validation, status reconciliation and refund tests.
- Crash/restart/replay and revoked-device tests.
- Signed PMS/Bridge APK pair verification and full folio/accounting E2E with the appliance.

**Non-negotiable boundary:** a hardware-backed signature proves only that a key signed a bounded payload. It does not prove customer payment. Only the appliance may authorize accounting after independent provider-evidence verification. No live payments, no self-activation, no trust-all TLS, and no claims of deployment or physical acceptance without test evidence.


Standalone Android companion application for Aurelia PMS. This repository owns native checkout UI, provider SDK adapters, and the authenticated Messenger IPC service consumed by Aurelia PMS.

## Current implementation status

This repository is **not yet a production payment application**. The current app implements a fail-closed, PMS-compatible protocol-v1 handshake only. It does not create charges, invoke provider SDKs, issue refunds, reconcile transactions, or report verified payments. The app advertises only `handshake`.

## PMS IPC contract

The service component is `com.aurelia.paybridge/.ipc.PayBridgeService`. It implements the wire contract in the PMS repository's `android/templates/PayBridgeProtocol.java` and `android/templates/PayBridgeClient.java`:

- PMS package: `com.aurelia.pms`
- Service permission: `com.aurelia.paybridge.permission.BIND_PAY_BRIDGE`
- Request: protocol range (`protocolMin`, `protocolMax`), caller package and nonce; response: selected protocol version, echoed nonce, bridge package and JSON capabilities (`capabilitiesJson`).
- The service checks that the Binder caller UID owns the expected PMS package and that PMS and Pay Bridge have the same Android signing identity.
- The Android signature permission also requires both APKs to use the same approved signing identity.

Do not use debug signing for a production pairing. Provision one controlled release signing identity and use it for both APKs. The production certificate and keystore must be supplied through protected release infrastructure, never committed to this repository.

## Security boundary

- **Never put provider API secrets, webhook secrets, PayU salt, PhonePe client secrets, or private keys in this APK.**
- PMS asks the Aurelia payment backend to create a payment session. The backend owns merchant credentials and provider API calls.
- The APK receives only short-lived, transaction-scoped checkout data (for example a Razorpay order ID or Cashfree payment session ID).
- SDK callbacks are UX signals only. The backend verifies provider status/webhooks; PMS posts only backend-verified payment events.
- This repository does not include production credentials, signing keys, or a live payment backend.

## SDK dependency inventory (not completed adapters)

- Razorpay Android Standard Checkout: `com.razorpay:checkout:1.6.41`
- Cashfree PG Android: `com.cashfree.pg:api:2.6.1`
- PayU Core Android: `in.payu:payu-sdk:7.12.3`
- PayU Custom Browser: `in.payu:payu-custom-browser:7.16.6`
- PayU UPI SDK: `in.payu:upisdk:1.8.8`
- PhonePe Intent SDK via PayU: `in.payu:phonepe-intent:1.8.9`

These dependencies are not proof of SDK resolution, runtime compatibility, or completed provider adapters. Keep only dependencies used by implemented and tested adapters before a production release.

## Build

Requires JDK 17 and Android SDK Platform 35.

Run the tests and build the debug APK:

```bash
./gradlew testDebugUnitTest assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Install it on a connected Android device with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The GitHub Actions workflow runs manifest verification, unit tests, debug APK assembly, and APK signature verification, then uploads the debug APK as the `aurelia-pay-bridge-debug-apk` artifact. A release APK requires a separately provisioned signing key in CI secrets; no keystore is committed. Debug signing is for development only and must not be used for production pairing.

## Production blockers

1. Agree and deploy the Aurelia payment backend API, merchant credential vault, webhook verification, status polling, idempotency and audit ledger.
2. Implement and sandbox-test provider adapters against merchant-enabled accounts; only then advertise provider capabilities.
3. Add a durable payment operation contract to both APKs with request IDs, session binding, strict validation, timeout/recovery and tests.
4. Provision matching PMS/Pay Bridge release signing, rotate certificates safely, and verify the installed pair on real supported Android devices.
5. Run CI, dependency resolution, Android instrumentation, provider sandbox and end-to-end PMS folio tests. No live charge should be enabled before all gates pass.

## Official provider references

- Razorpay: https://razorpay.com/docs/payments/payment-gateway/android-integration/standard/
- Cashfree: https://www.cashfree.com/docs/payments/online/mobile/android
- PayU Core: https://docs.payu.in/docs/integration-steps-android-core-sdk
- PayU Custom Browser: https://docs.payu.in/docs/integration-steps-android-customer-browser
- PayU PhonePe Intent: https://docs.payu.in/docs/integration-steps-android-phonepe-sdk

## Production release signing

Production release builds require the four protected GitHub Actions secrets: `AURELIA_RELEASE_KEYSTORE_BASE64`, `AURELIA_RELEASE_KEYSTORE_PASSWORD`, `AURELIA_RELEASE_KEY_ALIAS`, and `AURELIA_RELEASE_KEY_PASSWORD`. The release workflow fails closed if any are missing. Keep the original keystore and passwords backed up securely; the same signing identity must be used for Aurelia PMS and Aurelia Pay Bridge. Both release workflows pin the approved public SHA-256 certificate fingerprint (`562c833fcb8808b226d6b0c33633c9771a47ec63cca5c42d8ef16c4c7b92bf86`) and reject any APK signed by another certificate. Configure the exact same four keystore values in both repositories; the fingerprint is public certificate metadata, not a secret. The approved shared launcher artwork is `assets/aurelia-icon.jpg` and is installed by `scripts/install-launcher-icon.sh`. Debug APKs are not compatible with cross-app signature-protected IPC.

## Diagnosing an installed signer mismatch

If PMS displays `untrusted_signer`, compare the SHA-256 certificate digests of the **installed APKs**, not the keystore filename or the claim that the same keystore was selected. The PMS diagnostic reports the installed PMS and Pay Bridge fingerprints and the approved release fingerprint. A keystore may contain multiple aliases, an older APK may remain installed, and a distribution service may re-sign an APK.

For a clean release-pair test:

1. Download the PMS and Pay Bridge production release APKs from their respective successful GitHub Actions runs for the intended commits.
2. Verify both package IDs and run `apksigner verify --print-certs` on each APK.
3. Confirm both SHA-256 certificate digests exactly equal `562c833fcb8808b226d6b0c33633c9771a47ec63cca5c42d8ef16c4c7b92bf86`.
4. Only after both artifacts pass verification, uninstall the older PMS and Pay Bridge installations and install the two verified release APKs.
5. Retry the handshake and record the device result. CI success and matching certificates do not replace physical-device IPC verification.

Never disable the signature-level permission or signer checks to suppress this error.

