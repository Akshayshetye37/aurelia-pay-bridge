# Build and release

## Local debug build

1. Install JDK 17 and Android SDK Platform 35.
2. Generate and commit the Gradle wrapper using Gradle 8.11.1:
   `gradle wrapper --gradle-version 8.11.1`
3. Run `./gradlew testDebugUnitTest assembleDebug`.

The wrapper JAR is a binary and is not fabricated in this source-only bootstrap. Until the wrapper is committed, CI intentionally fails with an actionable message.

## Release signing

Do not commit a keystore. Configure protected GitHub Actions secrets and a release signing configuration before creating a production APK. Restrict release workflow permissions and require reviewed tags/releases.

## Before installing beside PMS

- Confirm package name and service component match the PMS client.
- Sign PMS and Pay Bridge with the same certificate if using the declared signature permission.
- Set `PMS_SIGNING_CERT_SHA256` to the exact PMS signing certificate digest.
- Confirm PMS permission declaration, handshake payload fields, and timeout/retry semantics against Aurelia-apk.
- Test on supported Android versions and OEM devices.
- Keep live merchant credentials exclusively in the backend vault.
