# Hardware-backed Bridge evidence key

## Implemented boundary

`HardwareEvidenceKey` creates a fresh P-256 / ECDSA signing key in Android Keystore using a one-time challenge issued by the appliance. The key is non-exportable and restricted to `PURPOSE_SIGN` with SHA-256. StrongBox is requested on Android 9+; only an explicit `StrongBoxUnavailableException` permits fallback to TEE. A software-backed result is rejected.

The returned enrollment object contains:
- SHA-256 key identifier derived from the X.509 SubjectPublicKeyInfo public key.
- Base64 public key.
- Android Keystore attestation certificate chain, leaf first.
- Reported security level (`STRONGBOX`, `TRUSTED_ENVIRONMENT`, or an older-API hardware-backed classification).

The server challenge must be 16–128 bytes, unpredictable, single-use, and tied to a pending owner-authorized enrollment. Creating an attested identity intentionally replaces the previous local key; key rotation therefore requires a new owner-authorized challenge and re-enrollment. Signing refuses to create an un-attested key implicitly.

## Appliance attestation verifier — required before activation

The appliance, not the Bridge, must validate the certificate chain and Android Key Attestation extension. At minimum it must:
1. Validate the full chain against maintained Google Android Key Attestation trust roots, including root rotation/revocation policy. Do not blindly trust a root merely because it is self-signed.
2. Require the attestation challenge to exactly equal the pending, unexpired, unused challenge issued for that enrollment.
3. Require both `attestationSecurityLevel` and `keymasterSecurityLevel` to be TrustedEnvironment or StrongBox; reject Software and Unknown.
4. Verify the attested application identity (package name and signing-certificate digest) against the approved Bridge release identity.
5. Enforce the expected verified-boot/device-lock policy and reject rollback or patch-level states outside the appliance's policy.
6. Verify that the leaf certificate public key matches the submitted public key, then atomically consume the challenge and bind the key to the owner-approved property/device record.

The Bridge currently exports the certificate chain but does not parse or validate the attestation extension. A returned chain is untrusted input until the appliance completes every check above. Google attestation roots and verification rules must be maintained server-side; do not pin a stale root in the APK.

## Signing boundary

The API signs exact byte arrays only. It does not yet define a production canonical evidence envelope. Before any signed payload can be accepted as evidence, the protocol must specify versioning, domain separation, deterministic serialization (canonical CBOR or a strict length-prefixed binary format), request/job ID, device ID, folio binding, amount in minor units, currency, provider reference, server nonce, issued/expiry timestamps, and replay rules.

The class does not create payment claims, infer provider success, contact the LAN appliance, or settle PMS folios. A hardware-backed signature proves only that the enrolled device key signed particular bytes. It does **not** prove that a provider collected money or that the statement is true. The appliance must independently verify provider evidence and bind it to its durable payment job before the PMS can post a folio credit.

## Tests and release status

The Android instrumentation test checks attested-key creation, returned leaf/public-key consistency, stable signing API operation, and public-key signature verification. CI currently compiles the instrumentation APK; it does not execute the test on a hardware-backed Android device. Emulators without TEE/StrongBox are not valid substitutes for the hardware acceptance test. A compatible physical-device run is still required.

## Deliberately not implemented yet

- Owner-authorized enrollment UI and appliance challenge/registration endpoints.
- Appliance-side Android attestation chain and extension verification.
- Device activation, revocation, and owner-authorized key rotation workflow.
- Canonical evidence envelope, request/job binding, monotonic sequence numbers, and server-side replay protection.
- Provider-side independent payment verification.
- mTLS client-certificate provisioning or LAN CA trust configuration.
- Payment execution IPC. Protocol v1 remains handshake-only and the service must not advertise payment capabilities.

## Network trust

Do not add a broad trust rule for a private IP, trust-all manager, user-installed CA dependency, or cleartext exception. The production appliance hostname and CA distribution model are not yet part of the checked-in project contract. Once those are fixed, configure a dedicated Android Network Security Config with the exact DNS hostname and pinned private root/intermediate CA, and test certificate rotation and hostname validation. Mutual TLS additionally requires client certificate enrollment and private-key lifecycle management; a trusted server CA alone is not mTLS.

## Release signing

The existing Bridge workflow reads the production keystore and passwords from GitHub Actions secrets, builds a release APK, verifies its APK signature, and compares the signer SHA-256 against the approved project fingerprint. Keep that pipeline rather than introducing a second signing action or duplicating secret names. Never commit a keystore, passwords, or private signing material.
