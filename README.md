# Aurelia Pay Bridge

Standalone Android companion application for **Aurelia PMS**. This repository owns the Android application, native PMS IPC endpoint, provider SDK integration work, security controls and Android build workflow.

## Project location

- **GitHub repository:** https://github.com/Akshayshetye37/aurelia-pay-bridge
- **Project root:** repository root `/` (local clone directory: `aurelia-pay-bridge/`)
- **Working branch:** `feature/payment-ipc-contract-foundation`
- **Default branch:** `main`
- **Open pull request:** https://github.com/Akshayshetye37/aurelia-pay-bridge/pull/1 (draft; not merged)
- **Android CI:** `.github/workflows/android.yml`

Clone:

```bash
git clone https://github.com/Akshayshetye37/aurelia-pay-bridge.git
cd aurelia-pay-bridge
git switch feature/payment-ipc-contract-foundation
```

## Repository structure

| Path | Purpose |
|---|---|
| `app/` | Android application module |
| `app/src/main/AndroidManifest.xml` | Permissions, package visibility, launcher activity and exported IPC service |
| `app/src/main/java/com/aurelia/paybridge/ipc/` | Messenger IPC protocol, service and nonce replay protection |
| `app/src/main/java/com/aurelia/paybridge/ui/` | Android UI |
| `app/src/test/` | JVM unit tests |
| `docs/` | Security, IPC contract, provider dependency and release documentation |
| `scripts/verify-merged-manifest.py` | Security-critical merged-manifest validation |
| `.github/workflows/android.yml` | Android CI and protected release build workflow |
| `build.gradle.kts`, `settings.gradle.kts` | Gradle project configuration |

## Current implementation status

**This is not a production payment application.** The current implementation provides a fail-closed protocol-v1 PMS handshake. It does not create payments, execute provider checkout, issue refunds, reconcile transactions or deliver verified payment events.

### Implemented foundation

- Android application shell and launcher.
- Exported Messenger IPC service protected by signature-level Android permission.
- Binder caller UID/package validation and PMS/Bridge signing-certificate comparison.
- Protocol-v1 handshake, protocol-range validation and bounded nonce replay protection.
- Dedicated IPC handler thread.
- PMS-compatible reserved protocol constants and regression tests.
- Merged-manifest security verification script.

### Not implemented / not production-ready

- Razorpay, Cashfree, PayU and PhonePe checkout adapters.
- Executable payment, status, refund, reconciliation and event IPC handlers.
- Aurelia Payment Backend API integration.
- Merchant credential vault, provider webhook verification, authoritative payment state, idempotency and reconciliation.
- End-to-end PMS folio settlement integration.
- Verified matching release-signature pairing on installed PMS and Bridge APKs.
- Provider sandbox certification and real-device end-to-end tests.

The Bridge advertises **only** the `handshake` capability. Reserved operation IDs and capability strings are not executable functionality.

## PMS IPC contract

- PMS package: `com.aurelia.pms`
- Pay Bridge package: `com.aurelia.paybridge`
- Service: `com.aurelia.paybridge/.ipc.PayBridgeService`
- Binding permission: `com.aurelia.paybridge.permission.BIND_PAY_BRIDGE`
- Handshake request/result: message IDs `100` / `101`

The contract mirrors the PMS repository's `android/templates/PayBridgeProtocol.java` and `PayBridgeClient.java`. The Bridge checks that the Binder caller UID owns the expected PMS package and that PMS and Bridge share an Android signing identity. The signature-level permission independently gates binding.

Do not use debug signing for production pairing. Both APKs must use the same controlled signing identity. Keystores and passwords must remain in protected release infrastructure, never in source control or chat.

## Payment architecture and security boundary

- PMS remains local-first and owns hotel operations, bookings, folios and hotel accounting.
- Pay Bridge is the native checkout companion, not the authoritative payment ledger.
- The central Aurelia Payment Backend is an optional deployment dependency at initial installation. Its base URL and tenant connection details must be configurable later from the app's Settings screen; installation and local PMS-to-Bridge connectivity must not require a server or cloud account. When no backend is configured or reachable, backend-dependent online payment actions remain unavailable and must fail closed. Once configured, the backend owns tenant-isolated provider credentials, payment sessions, provider API calls, webhook verification, authoritative payment/refund state, idempotency and reconciliation.
- Each hotel tenant uses its own authenticated account, property permissions and merchant/provider credentials.
- **Never put provider API secrets, webhook secrets, PayU salt, PhonePe client secrets or private keys in either APK.**
- PMS obtains a payment session from the backend; Bridge receives only short-lived transaction-scoped checkout data.
- SDK callbacks are UI signals, not proof of payment. PMS may settle a folio only after authoritative backend verification.
- Ambiguous outcomes remain pending/unknown until reconciled.
- Google Drive or local/server backup destinations are for backup and file exchange, not a live shared payment database.

The backend is a separate project/deployment responsibility. This repository does not contain a deployed payment backend or production provider credentials. Backend configuration is not a prerequisite for installing or opening Pay Bridge; the app must support a later setup flow in Settings and clearly show whether the backend is unconfigured, connected or unavailable. Do not substitute local-only or SDK callback data for authoritative payment verification.

## Provider SDK dependencies

The following are declared in `app/build.gradle.kts`. Dependency declaration does not prove resolution, runtime compatibility or an implemented adapter:

- Razorpay Android Standard Checkout: `com.razorpay:checkout:1.6.41`
- Cashfree PG Android: `com.cashfree.pg:api:2.6.1`
- PayU Core: `in.payu:payu-sdk:7.12.3`
- PayU Custom Browser: `in.payu:payu-custom-browser:7.16.6`
- PayU UPI SDK: `in.payu:upisdk:1.8.8`
- PhonePe Intent SDK via PayU: `in.payu:phonepe-intent:1.8.9`

Remove dependencies that are not used by implemented and tested adapters before production.

## Build

Requirements: JDK 17, Android SDK Platform 35 and Build Tools 35.0.0. The CI workflow bootstraps Gradle 8.11.1 and generates the wrapper during each run.

```bash
gradle wrapper --gradle-version 8.11.1
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`.

A debug APK is for development only and is not suitable for production pairing with a release-signed PMS APK.

## CI and release

The workflow performs merged-manifest validation, JVM unit tests and debug APK assembly. Push builds additionally require these protected GitHub Actions secrets before release signing and APK signature verification:

- `AURELIA_RELEASE_KEYSTORE_BASE64`
- `AURELIA_RELEASE_KEYSTORE_PASSWORD`
- `AURELIA_RELEASE_KEY_ALIAS`
- `AURELIA_RELEASE_KEY_PASSWORD`

Successful push builds attempt to publish a production-signed APK as a **draft prerelease GitHub Release**. A draft release is for controlled testing, not an assertion of production readiness.

**CI status (2026-11-01):** GitHub Actions run [#84](https://github.com/Akshayshetye37/aurelia-pay-bridge/actions/runs/41244218181), attempt 2, completed successfully on commit `17ab8982e1a5f5e499a395790e57c382edaf9722`. Both jobs passed: merged-manifest validation, JVM unit tests, debug assembly, production signing configuration validation, release APK build, APK package/signature verification, artifact upload and draft release publication. This verifies the CI build and APK signature process for that commit; it does **not** establish PMS certificate matching, device-level IPC operation, provider certification or production payment readiness. Check the live [Actions runs](https://github.com/Akshayshetye37/aurelia-pay-bridge/actions) for newer revisions.

## Production completion gates

1. Finalize and deploy the central Aurelia Payment Backend with tenant isolation, merchant credential vault, webhook verification, payment state, idempotency, audit and reconciliation.
2. Implement provider adapters and test each against provider sandbox accounts.
3. Implement executable IPC operations in both PMS and Bridge; advertise a capability only after the handler is implemented and verified.
4. Use matching controlled release signing for both APKs and verify installed certificate fingerprints.
5. Complete payment lifecycle, timeout, process-death, duplicate request, refund, reconciliation and PMS folio tests.
6. Pass CI, dependency resolution, Android device tests and provider sandbox end-to-end tests before enabling live charges.

## Documentation

- [Security model](docs/SECURITY_MODEL.md)
- [IPC integration verification v1](docs/IPC_INTEGRATION_VERIFICATION_V1.md)
- [Reserved payment IPC contract](docs/RESERVED_PAYMENT_IPC_CONTRACT.md)
- [Provider SDK matrix](docs/PROVIDER_SDK_MATRIX.md)
- [Build and release](docs/BUILD_AND_RELEASE.md)

## Official provider references

- Razorpay: https://razorpay.com/docs/payments/payment-gateway/android-integration/standard/
- Cashfree: https://www.cashfree.com/docs/payments/online/mobile/android
- PayU Core: https://docs.payu.in/docs/integration-steps-android-core-sdk
- PayU Custom Browser: https://docs.payu.in/docs/integration-steps-android-customer-browser
- PayU PhonePe Intent: https://docs.payu.in/docs/integration-steps-android-phonepe-sdk
