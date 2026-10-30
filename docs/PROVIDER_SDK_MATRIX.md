# Provider SDK matrix

The dependencies below are taken from provider-maintained documentation or provider-owned distribution instructions. A Gradle dependency does not mean the adapter is complete or the merchant is enabled.

| Provider | Android artifact | Pinned version | Source | Adapter status |
|---|---|---:|---|---|
| Razorpay | com.razorpay:checkout | 1.6.41 | https://razorpay.com/docs/payments/payment-gateway/android-integration/standard/ | Dependency only |
| Cashfree | com.cashfree.pg:api | 2.6.1 | https://www.cashfree.com/docs/payments/online/mobile/android | Dependency only |
| PayU Core | in.payu:payu-sdk | 7.12.3 | https://docs.payu.in/docs/integration-steps-android-core-sdk | Dependency only |
| PayU Custom Browser | in.payu:payu-custom-browser | 7.16.6 | https://docs.payu.in/docs/integration-steps-android-customer-browser | Dependency only |
| PayU UPI | in.payu:upisdk | 1.8.8 | https://docs.payu.in/docs/integration-steps-android-customer-browser | Optional dependency; verify applicable PayU integration |
| PhonePe Intent via PayU | in.payu:phonepe-intent | 1.8.9 | https://docs.payu.in/docs/integration-steps-android-phonepe-sdk | Dependency only |

## Deliberately not included

PhonePe's Java PG SDK is a server-side SDK, not an Android client SDK. It belongs in the Aurelia payment backend if that backend adopts it. Stripe and other providers are not added until their current official Android integration path, artifact coordinates, version, and intended checkout flow have been verified. Avoid adding every provider SDK to the APK: it increases APK size, dependency conflicts, update burden, and attack surface.

## Integration rules

- Create provider orders on the Aurelia backend.
- Pass only the provider-issued order/session token and required public key identifier to the Android SDK.
- Keep PayU hash generation and all provider secrets server-side.
- For Cashfree, call the backend create-order endpoint and use its payment session ID; verify status server-side after callback.
- For Razorpay, use a backend-created order ID; verify signature/status on backend and confirm capture.
- Keep provider callbacks as non-authoritative UX signals.
- Advertise a provider capability only after the adapter is implemented, tested in sandbox, and enabled for the merchant.
