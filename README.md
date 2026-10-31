# Aurelia Pay Bridge

Standalone Android companion application for **Aurelia PMS**. The project repository owns the Android application, native payment-bridge IPC endpoint, provider SDK integration work, security controls and build/release workflow.

## Project location

- **GitHub repository:** https://github.com/Akshayshetye37/aurelia-pay-bridge
- **Working branch:** `feature/payment-ipc-contract-foundation`
- **Default branch:** `main`
- **Current working-branch commit:** `4504505ba23cbdb2e7bbbf28af3d4a966782ea80`
- **Open pull request:** https://github.com/Akshayshetye37/aurelia-pay-bridge/pull/1 (draft; not merged)
- **CI workflow:** [Android CI](.github/workflows/android.yml)

The repository root is the project root. When cloned locally, the directory is wherever the user chooses to clone it; there is no fixed local computer path recorded in this repository.

Clone and enter the project:

```bash
git clone https://github.com/Akshayshetye37/aurelia-pay-bridge.git
cd aurelia-pay-bridge
git switch feature/payment-ipc-contract-foundation
```

## Repository structure

| Path | Purpose |
|---|---|
| `app/src/main/AndroidManifest.xml` | Android permissions, package visibility, launcher activity and exported Pay Bridge service |
| `app/src/main/java/com/aurelia/paybridge/ipc/` | IPC protocol, Messenger service and nonce replay protection |
| `app/src/main/java/com/aurelia/paybridge/ui/` | Android application UI |
| `app/src/main/res/` | Android layouts and resources |
| `app/src/test/java/com/aurelia/paybridge/ipc/` | JVM unit tests for protocol and replay protection |
| `docs/SECURITY_MODEL.md` | Security boundaries and trust model |
| `docs/IPC_INTEGRATION_VERIFICATION_V1.md` | Handshake integration verification |
| `docs/RESERVED_PAYMENT_IPC_CONTRACT.md` | Reserved PMS/payment message contract; not executable payment functionality |
| `docs/PROVIDER_SDK_MATRIX.md` | Provider SDK dependency and integration inventory |
| `docs/BUILD_AND_RELEASE.md` | Build and release guidance |
| `scripts/verify-merged-manifest.py` | Security-critical merged-manifest validation |
| `.github/workflows/android.yml` | CI, release signing, APK verification and draft release publishing |

## Current implementation status

**This is not a production payment application.** The current implementation provides a fail-closed, PMS-compatible protocol-v1 handshake. It does not yet create payments, execute provider checkout, issue refunds, reconcile transactions, or deliver verified payment events.

The bridge advertises only the `handshake` capability. Payment, refund, reconciliation, payment-event and provider-management identifiers are reserved contract constants only. They are not executable operations and must not be treated as implemented features.

### Implemented foundation

- Android application shell and launcher.
- Exported Messenger IPC service protected by a signature-level Android permission.
- Caller UID/package validation and matching PMS/Pay Bridge signing-certificate check.
- Protocol-v1 handshake with protocol-range validation and nonce replay protection.
- Dedicated IPC handler thread.
- Contract constants and tests mirrored against the PMS-side protocol.
- Merged-manifest security verification script.
- CI workflow configured for unit tests, debug APK assembly and production-signed release APK verification.

### Not implemented / not production-ready

- Razorpay, Cashfree, PayU and PhonePe checkout adapters.
- Executable payment, payment-status, refund, reconciliation and event IPC handlers.
- Aurelia Payment Backend API integration.
- Merchant credential management or provider secret storage in this APK.
- Provider webhook verification, authoritative payment state, durable retry/outbox and reconciliation.
- End-to-end PMS folio settlement integration.
- Verified matching release-signature pairing on installed PMS and Pay Bridge APKs.
- Provider sandbox certification and real-device end-to-end payment tests.

## PMS IPC contract

The service component is `com.aurelia.paybridge/.ipc.PayBridgeService`. It mirrors the contract in the PMS repository's `android/templates/PayBridgeProtocol.java` and is consumed by `android/templates/PayBridgeClient.java`.

- PMS package: `com.aurelia.pms`
- Pay Bridge package: `com.aurelia.paybridge`
- Service permission: `com.aurelia.paybridge.permission.BIND_PAY_BRIDGE`
- Handshake request/result message IDs: `100` / `101`
- Reserved payment/refund/reconciliation/event message IDs are documented in `docs/RESERVED_PAYMENT_IPC_CONTRACT.md`.

The service checks that the Binder caller UID owns the expected PMS package and that PMS and Pay Bridge have the same Android signing identity. The Android signature permission independently restricts binding to applications signed with the same approved certificate.

Do not use debug signing for a production pairing. One controlled release signing identity must be used for both APKs. Keystores and passwords must be stored in protected release infrastructure, never committed to this repository or pasted into chat.

## Payment architecture and security boundary

- Aurelia PMS remains local-first and owns hotel operations, bookings, folios and hotel accounting.
- Aurelia Pay Bridge is the native Android checkout companion. It must not become the authoritative payment ledger.
- The Aurelia Payment Backend is intended to own tenant-isolated provider credentials, payment sessions, provider API calls, webhook verification, authoritative payment/refund status, idempotency and reconciliation.
- Each hotel tenant must use its own authenticated account, property permissions and merchant/provider credentials.
- **Never put provider API secrets, webhook secrets, PayU salt, PhonePe client secrets or private keys in either APK.**
- PMS requests a payment session from the backend. Pay Bridge receives only short-lived, transaction-scoped checkout data.
- SDK callbacks are user-interface signals, not proof of payment. PMS must settle a folio only after authoritative backend verification.
- A lost or ambiguous callback must remain pending/unknown until reconciled; it must never be assumed paid or failed.
- Google Drive or local/server backup destinations are for backup and file exchange, not a live shared payment database.

The backend is a separate project/deployment responsibility. This repository does not contain a deployed payment backend or production provider credentials.

## SDK dependency inventory

The following dependencies are declared in `app/build.gradle.kts`; declaration does not prove that an adapter is implemented, that dependencies resolve in CI, or that SDKs are compatible at runtime.

- Razorpay Android Standard Checkout: `com.razorpay:checkout:1.6.41`
- Cashfree PG Android: `com.cashfree.pg:api:2.6.1`
- PayU Core Android: `in.payu:payu-sdk:7.12.3`
- PayU Custom Browser: `in.payu:payu-custom-browser:7.16.6`
- PayU UPI SDK: `in.payu:upisdk:1.8.8`
- PhonePe Intent SDK via PayU: `in.payu:phonepe-intent:1.8.9`

Keep only dependencies required by implemented and tested adapters before production release.

## Local build

Requirements: JDK 17, Android SDK Platform 35 and Android build tools 35.0.0. The CI workflow generates the Gradle wrapper using Gradle 8.11.1.

Run:

```bash
gradle wrapper --gradle-version 8.11.1
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Debug APK output:

```
app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

A debug APK is for development only. It is not suitable for production pairing with a release-signed PMS APK.

## CI and release status

The workflow at `.github/workflows/android.yml` performs merged-manifest validation, JVM unit tests and debug APK assembly. For push builds (not pull requests), it also requires the protected signing secrets, builds a release APK, verifies its APK signature and attempts to publish the APK as a **draft prerelease GitHub Release**.

Required GitHub Actions secrets:

- `AURELIA_RELEASE_KEYSTORE_BASE64`
- `AURELIA_RELEASE_KEYSTORE_PASSWORD`
- `AURELIA_RELEASE_KEY_ALIAS`
- `AURELIA_RELEASE_KEY_PASSWORD`

The current working-branch commit has **not passed CI**. Build #53 (run ID `41228163301`) failed on three attempts; the GitHub job-log download returned `BlobNotFound`, so the failing step has not yet been identified. No successful release APK for the current commit has been verified. Do not treat the earlier successful build #47 as a build of the current source.

Latest run: https://github.com/Akshayshetye37/aurelia-pay-bridge/actions/runs/41228163301

## Production completion gates

1. Confirm and deploy the central Aurelia Payment Backend API and its tenant isolation, merchant credential vault, webhook verification, payment state, idempotency, audit and reconciliation services.
2. Implement provider adapters and test each one against provider sandbox accounts.
3. Implement and test executable IPC operations consistently in both PMS and Pay Bridge; advertise a capability only after its handler is implemented and verified.
4. Complete matching release signing for both APKs and verify the signing-certificate fingerprints of the actual installed APKs.
5. Complete payment lifecycle, timeout, process-death, duplicate request, refund, reconciliation and PMS folio tests.
6. Pass CI, dependency resolution, Android device tests and provider sandbox end-to-end tests before enabling live charges.

## Official provider references

- Razorpay: https://razorpay.com/docs/payments/payment-gateway/android-integration/standard/
- Cashfree: https://www.cashfree.com/docs/payments/online/mobile/android
- PayU Core: https://docs.payu.in/docs/integration-steps-android-core-sdk
- PayU Custom Browser: https://docs.payu.in/docs/integration-steps-android-customer-browser
- PayU PhonePe Intent: https://docs.payu.in/docs/integration-steps-android-phonepe-sdk
