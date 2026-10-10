package app.rasoi.core.model

data class User(val id: UserId, val phone: PhoneNumber, val displayName: String, val upiVpa: String? = null)
