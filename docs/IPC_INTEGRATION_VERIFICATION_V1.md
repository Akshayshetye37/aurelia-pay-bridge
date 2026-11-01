# Aurelia IPC Integration Verification v1

## Purpose and release gate

Verify the installed Aurelia PMS and Aurelia Pay Bridge Android applications communicate through the protocol-v1 explicit bound-service Messenger handshake.

This is an inter-app IPC test on one Android device. It is not device-to-device communication.

Passing repository CI or comparing source files does not pass this verification. The milestone is complete only after the two installed APKs pass the device test matrix below and the evidence is recorded.

## Current implementation scope

- PMS package: `com.aurelia.pms`
- Bridge package: `com.aurelia.paybridge`
- Bridge service: `com.aurelia.paybridge.ipc.PayBridgeService`
- Binding permission: `com.aurelia.paybridge.permission.BIND_PAY_BRIDGE`
- Protocol: v1
- Request/response: Messenger message 100 / 101
- Advertised capability: `handshake` only
- Payment, refund, provider configuration and reconciliation operations: not implemented

## Signing prerequisite

Android signature-level permissions require both APKs to be signed by the same signing identity.

Do not assume separately generated CI debug APKs satisfy this condition. Independent GitHub runners normally generate/use different debug keystores. For the device test, build both APKs using one controlled test keystore, or use a controlled release signing identity. Do not commit the keystore or passwords to either repository.

Before installation, inspect both APK certificates with Android SDK `apksigner verify --print-certs` and record the SHA-256 certificate digest. The digests must match exactly.

## Build and installation

1. Wait for a successful Pay Bridge GitHub Actions run on the selected commit. For the release-pair test, use the production-signed release artifact and confirm the workflow's certificate-verification step passed; a debug APK is not an acceptable substitute.
2. Build PMS from the verified `feature/multi-provider-payment-backend` branch HEAD (or the explicitly selected PMS integration commit recorded for the test).
3. Sign both APKs with the same controlled test certificate.
4. Verify the package IDs and certificate digests.
5. Install both APKs on the same Android device:
   - `adb install -r aurelia-pay-bridge.apk`
   - `adb install -r aurelia-pms.apk`
6. Confirm installation:
   - `adb shell pm path com.aurelia.paybridge`
   - `adb shell pm path com.aurelia.pms`

The exact APK paths depend on the build output; do not assume the CI debug artifacts are mutually trusted.

## Positive test matrix

| ID | Test | Expected result |
|---|---|---|
| P01 | Install both correctly signed APKs | Both packages install |
| P02 | Launch PMS with Bridge installed | PMS discovers Bridge |
| P03 | PMS binds to explicit Bridge service | Binding succeeds |
| P04 | Android signature permission | Binding is permitted for matching signer |
| P05 | Binder UID and PMS package check | Bridge authorizes PMS caller |
| P06 | Shared signer check | Bridge accepts matching signer |
| P07 | Protocol-v1 handshake | Request 100 receives response 101 |
| P08 | Nonce echo | PMS accepts only exact outstanding nonce |
| P09 | Capability response | PMS accepts unique string array containing `handshake` |
| P10 | Connection indicator | PMS reports authenticated/connected only after valid response |
| P11 | Kill Bridge process while PMS remains open | PMS clears authenticated state |
| P12 | Relaunch Bridge / reconnect | PMS rebinds and completes a fresh handshake |
| P13 | Manually disconnect in PMS | PMS does not automatically reconnect until explicit user retry |

## Negative test matrix

Perform these only on a test device and test builds.

| ID | Test | Expected result |
|---|---|---|
| N01 | Install Bridge signed with a different certificate | PMS refuses trusted connection; signature permission also blocks protected binding |
| N02 | Request with unsupported protocol range | Bridge does not return a successful handshake |
| N03 | Request with malformed/short nonce | Bridge rejects request |
| N04 | Replay the same nonce during one Bridge process lifetime | Bridge rejects duplicate |
| N05 | PMS receives a response with wrong nonce | PMS rejects connection |
| N06 | PMS receives wrong package name or protocol version | PMS rejects connection |
| N07 | PMS receives malformed capabilities or no `handshake` capability | PMS rejects connection |
| N08 | Delay bind/handshake beyond 10 seconds | PMS times out, clears connecting state and follows bounded retry policy |
| N09 | Saturate replay cache with unexpired nonces | New handshakes fail closed; existing unexpired nonce records are not evicted |
| N10 | Restart Bridge process | In-memory replay cache resets; document this limitation and verify a new random PMS nonce is used |

A wrong-signer build may be blocked at Android's permission layer before the Bridge service can log a caller rejection. That is expected; do not interpret the absence of a service log as a failed permission test.

## Evidence capture

CI additionally parses the actual AGP merged debug manifest and asserts that the PMS package visibility query, exported service, signature permission, launcher activity, INTERNET permission, application label, theme, backup setting and cleartext setting survive manifest merging. CI also runs a same-nonce concurrent-request unit test; exactly one request may be accepted.\n\nCapture and retain:

- Git commit SHA for each repository.
- GitHub Actions run URLs and final conclusions.
- APK package IDs and version codes.
- APK certificate SHA-256 digests.
- Android device model and Android version.
- Test matrix result for every ID (pass/fail/not run).
- Relevant filtered logcat output:
  - `adb logcat -c`
  - reproduce one test
  - `adb logcat -d -s AureliaPayBridge`
- PMS UI state screenshots for connected, disconnected and reconnecting states.

Do not include secrets, payment credentials, personal guest data or full device identifiers in the evidence bundle.

## Pass criteria

The milestone passes only if:

1. Pay Bridge CI unit tests and APK build pass on the exact recorded HEAD.
2. PMS CI build passes on the exact recorded HEAD.
3. Both installed APKs have matching approved signing certificate digests.
4. All positive tests P01-P13 pass.
5. All applicable negative tests N01-N10 fail closed as specified.
6. No payment operation is exposed or advertised by protocol v1.

If physical-device tests have not been run, status must remain **SOURCE/CI VERIFIED — DEVICE IPC NOT VERIFIED**.
