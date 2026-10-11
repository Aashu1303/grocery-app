# 0004. Firebase for phone auth and push

Status: Accepted
Date: 2026-10-10

## Context
Flatmates and cooks are identified by phone number in India. Push is needed for approvals and expiry alerts.

## Decision
Firebase Auth phone OTP, with the server verifying the ID token behind a `TokenVerifier` interface. FCM for push behind a `PushSender` interface. Tests use fakes of both.

## Consequences
Local development and tests need no Firebase project. Production needs Firebase credentials supplied by environment variables.
