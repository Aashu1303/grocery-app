package app.rasoi.core.domain.auth

import app.rasoi.core.model.PhoneNumber

data class OtpRequest(val phone: PhoneNumber, val verificationId: String)
