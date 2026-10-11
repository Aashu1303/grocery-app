package app.rasoi.server.auth

import app.rasoi.core.model.PhoneNumber

/** Who an ID token says the caller is, after its signature and claims were verified. */
data class VerifiedIdentity(val firebaseUid: String, val phone: PhoneNumber)
