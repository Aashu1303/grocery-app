package app.rasoi.core.contract

import kotlinx.serialization.json.Json

/** The single JSON configuration shared by the app and the server. */
val ContractJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}
