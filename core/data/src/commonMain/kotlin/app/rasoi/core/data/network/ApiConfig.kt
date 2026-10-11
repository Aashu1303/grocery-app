package app.rasoi.core.data.network

/** Where the Rasoi server lives; supplied by each platform's build configuration. */
data class ApiConfig(val baseUrl: String) {
    init {
        require(baseUrl.startsWith("https://") || baseUrl.startsWith("http://")) { "baseUrl must be an http(s) URL" }
    }
}
