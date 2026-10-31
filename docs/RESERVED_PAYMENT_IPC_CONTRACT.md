# Reserved payment IPC contract — foundation only

Status: **protocol v1 handshake only; payment/refund/status/reconciliation operations are reserved and disabled.**

This document records the operation identifiers shared with the PMS contract at
`android/templates/PayBridgeProtocol.java`. It is a compatibility foundation,
not an executable payment protocol or evidence of a working payment backend.

## PMS dependency parity

The Bridge protocol constants mirror the PMS Java contract, including the
explicit service class, package identity, bind action, protocol range and
handshake message names. The manifest exports only the named
`PayBridgeService`, protected by the signature-level binding permission.

PMS also gates provider configuration on `provider.management`. Pay Bridge
defines that capability identifier but deliberately does **not** advertise it:
there is no provider-management IPC handler or backend-backed provider
configuration operation yet. This prevents PMS from exposing a feature the
Bridge cannot execute.

## Implemented now

- Explicit bound-service Messenger handshake (request 100 / response 101).
- Caller UID/package and shared signing identity checks.
- Signature-level Android service permission.
- Bounded, process-lifetime nonce replay protection.
- Dedicated IPC HandlerThread.
- Capability response advertises exactly `handshake`.

## Reserved message identifiers

| Operation | Request | Response / acknowledgement | Current status |
|---|---:|---:|---|
| Create payment | 200 | 201 | Reserved; rejected |
| Get payment status | 202 | 203 | Reserved; rejected |
| Request refund | 300 | 301 | Reserved; rejected |
| Get refund status | 302 | 303 | Reserved; rejected |
| Reconcile payment | 400 | 401 | Reserved; rejected |
| Payment event | 500 | 501 acknowledgement | Reserved; rejected |

The service currently handles only message 100. It returns `false` for every
other message, including any future provider-management operation. Do not change this until the PMS client and trusted backend are
ready to implement the same reviewed contract.

## Reserved payload keys

The identifiers mirrored from PMS are:

- Correlation and ownership: `requestId`, `transactionId`, `bookingId`, `folioId`.
- Money: `amountPaise`, `currency`.
- Provider correlation: `providerId`, `merchantReference`.
- Result metadata: `status`, `evidenceId`.

Limits mirrored from PMS: request ID 128 characters, references 128 characters,
maximum message payload 64 KiB, currency constant `INR`.

These constants do not authorize trusting any value supplied by PMS. When
operations are implemented, the backend-issued session must be authoritative
for amount, currency, booking, folio, provider and merchant reference. The
Bridge must validate bounded inputs and must not be able to mark a folio paid.

## Capability negotiation rule

Reserved capabilities are defined for contract parity but are not included in
the service's advertised capability list:

- `payment.create`
- `payment.status`
- `refund.create`
- `refund.status`
- `payment.reconciliation`
- `payment.events`

A capability may be advertised only after its handler, authentication,
request correlation, timeout behavior, duplicate handling, durable recovery,
tests and backend integration are implemented. Merely defining a message ID
or SDK dependency is not implementation.

## Required gates before executable operations

1. Confirm the trusted backend repository, API base URL, authentication issuer,
   tenant/property authorization model and deployed API contract.
2. Agree request/response/event schemas with the PMS implementation and add
   cross-repository contract tests.
3. Add authenticated session-bound IPC, request IDs, strict validation,
   bounded payloads, deadlines, cancellation semantics and replay protection.
4. Persist Bridge execution state before launching a provider SDK. Treat a lost
   callback or process death after dispatch as `UNKNOWN`, never as failure.
5. Treat SDK callbacks as UX signals only. The backend verifies provider status;
   only a backend-verified result may be relayed as verified to PMS.
6. Add provider adapters one at a time after merchant onboarding and sandbox
   tests. Do not ship provider secrets or webhook secrets in this APK.
7. Test both APKs installed on the same device, signed by the same approved
   certificate, including process death, duplicate events, network loss and
   unknown outcomes.

## Data ownership

- PMS owns reservations, folios and hotel accounting.
- Pay Bridge owns native checkout execution and its installation-local
  execution/recovery state.
- The trusted backend owns canonical payment/refund state, provider credentials,
  idempotency, webhook verification and reconciliation.

Pay Bridge must not become a second authoritative financial ledger. Its local
records are execution evidence and recovery aids, not the source of truth for
whether a payment was captured.
