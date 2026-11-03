# Hardware-backed Bridge evidence key

## Implemented boundary

`HardwareEvidenceKey` creates a P-256 / ECDSA signing key in Android Keystore and refuses to use a key unless Android reports it is inside secure hardware (TEE or StrongBox). The private key is non-exportable. The public key is exposed as X.509 SubjectPublicKeyInfo bytes, Base64 encoded, with a SHA-256 key identifier.

The API signs exact byte arrays only. The caller is responsible for supplying bytes produced by a future versioned canonical evidence encoder. The class does not create payment claims, infer provider success, contact the LAN appliance, or settle PMS folios.

An instrumentation test on a physical/emulated Android runtime checks key persistence, public-key signature verification, and successful hardware-backed key initialization. Emulator support varies; the hardware-backed assertion is expected to fail on emulators without a secure hardware implementation. A real-device run remains mandatory.

## Deliberately not implemented yet

- Remote device enrollment, server-side public-key registration, certificate-chain attestation, key revocation, or rotation.
- Canonical evidence envelope schema, request/job binding, monotonic sequence numbers, or replay protection for payment evidence.
- Provider-side independent payment verification.
- mTLS client-certificate provisioning or LAN CA trust configuration.
- Payment execution IPC. Protocol v1 remains handshake-only and the service must not advertise payment capabilities.

A hardware-backed signature proves only that a statement was signed by the enrolled device key. It does **not** prove that a provider collected money or that the statement is true. The LAN appliance must independently verify provider evidence and bind it to its durable payment job before the PMS can post a folio credit.

## Network trust

Do not add a broad trust rule for a private IP, trust-all manager, user-installed CA dependency, or cleartext exception. The production appliance hostname and CA distribution model are not yet part of the checked-in project contract. Once those are fixed, configure a dedicated Android Network Security Config with the exact DNS hostname and pinned private root/intermediate CA, and test certificate rotation and hostname validation. Mutual TLS additionally requires client certificate enrollment and private-key lifecycle management; a trusted server CA alone is not mTLS.

## Release signing

The existing Bridge workflow already reads the production keystore and passwords from GitHub Actions secrets, builds a release APK, verifies its APK signature, and compares the signer SHA-256 against the approved project fingerprint. Keep that pipeline rather than introducing a second signing action or duplicating secret names. Never commit a keystore, passwords, or private signing material.
