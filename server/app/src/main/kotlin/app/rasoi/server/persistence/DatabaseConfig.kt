package app.rasoi.server.persistence

data class DatabaseConfig(val url: String, val user: String, val password: String, val maxPoolSize: Int = 10) {
    override fun toString(): String = "DatabaseConfig(url=$url, user=$user, maxPoolSize=$maxPoolSize)"
}
