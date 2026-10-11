package app.rasoi.core.data.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

internal actual fun platformEngine(): HttpClientEngine = Darwin.create()
