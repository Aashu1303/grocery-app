package app.rasoi.core.contract.devices

import kotlinx.serialization.Serializable

/** Registers the phone's push token so the server can send it alerts; the same token may be sent again safely. */
@Serializable
data class RegisterDeviceRequestDto(val token: String, val platform: DevicePlatformDto)
