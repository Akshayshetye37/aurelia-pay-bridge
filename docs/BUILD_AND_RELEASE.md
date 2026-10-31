# Build and release

## Build environment

- JDK 17
- Android SDK Platform 35
- Android Build Tools 35.0.0
- Gradle 8.11.1
- Android Gradle Plugin 8.9.1
- Kotlin Android plugin 2.1.20

The repository does not currently contain the Gradle wrapper files. CI bootstraps Gradle 8.11.1 and generates a wrapper on the runner before invoking `./gradlew`. For a local build, either install Gradle 8.11.1 and generate the wrapper, or use the Gradle installation directly:

```bash
gradle wrapper --gradle-version 8.11.1
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## CI

The Android CI workflow runs merged-manifest validation, JVM unit tests and debug APK assembly. Pull-request runs do not use production signing secrets.

Push builds additionally require all of these protected GitHub Actions secrets:

- `AURELIA_RELEASE_KEYSTORE_BASE64`
- `AURELIA_RELEASE_KEYSTORE_PASSWORD`
- `AURELIA_RELEASE_KEY_ALIAS`
- `AURELIA_RELEASE_KEY_PASSWORD`

The workflow decodes the keystore into the runner temporary directory, builds the release variant, verifies the APK signature with `apksigner`, and attempts to publish the resulting APK as a draft prerelease. Do not upload a keystore to the repository or print secrets in CI logs.

## Signing boundary

Both PMS and Pay Bridge must be signed by the same approved certificate for the declared Android signature permission to allow their IPC binding. Separately generated default debug keystores do not establish that trust relationship. Verify certificate SHA-256 digests on the actual APKs before the device IPC test.

Production signing uses one controlled release identity. Store the original keystore and credentials in protected release infrastructure and maintain a secure recovery backup. Do not put keystore files or passwords in source control.

## Before installing beside PMS

- Confirm package names and the explicit service component match the PMS client.
- Confirm PMS declares and requests `com.aurelia.paybridge.permission.BIND_PAY_BRIDGE`.
- Verify both APK certificate SHA-256 digests match.
- Confirm handshake fields, nonce echo, protocol version, timeout and retry behavior against the exact PMS source commit.
- Run the positive and negative matrix in `IPC_INTEGRATION_VERIFICATION_V1.md` on the same physical Android device.
- Keep provider merchant credentials exclusively in the trusted backend.

## Release status

A CI failure or missing job log is not a successful build. Record the exact source commit, workflow run, job conclusion, artifact and signing certificate digest for every candidate release. A draft prerelease is for controlled testing only. Payment execution and provider certification remain separate release gates.
