# Aurelia Pay Bridge

Standalone Android companion application for Aurelia PMS. This repository owns native checkout UI, provider SDK adapters, and the authenticated Messenger IPC service consumed by Aurelia PMS.

## Security boundary

- **Never put provider API secrets, webhook secrets, PayU salt, PhonePe client secrets, or private keys in this APK.**
- PMS asks the Aurelia payment backend to create a payment session. The backend owns merchant credentials and provider API calls.
- The APK receives only short-lived, transaction-scoped checkout data (for example a Razorpay order ID or Cashfree payment session ID).
- SDK callbacks are UX signals only. The backend verifies provider status/webhooks; PMS posts only backend-verified payment events.
- This repository does not include production credentials, signing keys, or a live payment backend.

## Current baseline

- Android application ID: `com.aurelia.paybridge`
- IPC service: `com.aurelia.paybridge/.ipc.PayBridgeService`
- Protocol: version 1 (handshake only in this baseline)
- Min SDK: 23; compile/target SDK: 35
- Provider SDK dependencies are pinned to versions confirmed in the linked provider documentation:
  - Razorpay Android Standard Checkout: `com.razorpay:checkout:1.6.41`
  - Cashfree PG Android: `com.cashfree.pg:api:2.6.1`
  - PayU Core Android: `in.payu:payu-sdk:7.12.3`
  - PayU Custom Browser: `in.payu:payu-custom-browser:7.16.6`
  - PayU UPI SDK: `in.payu:upisdk:1.8.8` (only if UPI Intent/Collect support is enabled)
  - PhonePe Intent SDK used by PayU integration: `in.payu:phonepe-intent:1.8.9` from PhonePe's official Maven repository.
- These are dependencies, not completed provider adapters. Each adapter must be implemented against its provider's current integration guide and verified with sandbox credentials before being advertised as supported.

## Build

Requires JDK 17 and Android SDK Platform 35.

```bash
./gradlew testDebugUnitTest assembleDebug
```

The GitHub Actions workflow builds the debug APK. A release APK requires a separately provisioned signing key in CI secrets; no keystore is committed.

## Required PMS alignment

The PMS native client currently expects the service component `com.aurelia.paybridge/.ipc.PayBridgeService`, protocol v1, a valid SHA-256 signing-certificate fingerprint, and a `handshake` capability. Confirm the exact PMS-side permission, allowed PMS signing certificate, and IPC message schema against `Aurelia-apk` before connecting real devices. The service fails closed until the PMS signing-certificate allowlist is configured.

## Provider source references

- Razorpay Android Standard SDK: https://razorpay.com/docs/payments/payment-gateway/android-integration/standard/
- Cashfree Android SDK: https://www.cashfree.com/docs/payments/online/mobile/android
- PayU Android Core SDK: https://docs.payu.in/docs/integration-steps-android-core-sdk
- PayU Android Customer Browser: https://docs.payu.in/docs/integration-steps-android-customer-browser
- PayU PhonePe Intent SDK instructions: https://docs.payu.in/docs/integration-steps-android-phonepe-sdk

Provider documentation and artifact versions can change. Revalidate before upgrading or releasing.
