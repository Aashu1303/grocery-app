package app.rasoi.core.contract.auth

import kotlinx.serialization.Serializable

/** Sets or clears the caller's UPI address, which flatmates use to pay them. */
@Serializable
data class UpdateProfileRequestDto(val upiVpa: String? = null)
