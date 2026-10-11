package app.rasoi.core.data.network

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

internal inline fun <reified B> HttpRequestBuilder.jsonBody(body: B) {
    contentType(ContentType.Application.Json)
    setBody(body)
}
