package app.rasoi.server.demo

import io.ktor.server.application.Application
import io.ktor.server.application.plugin
import io.ktor.server.routing.HttpMethodRouteSelector
import io.ktor.server.routing.PathSegmentConstantRouteSelector
import io.ktor.server.routing.PathSegmentParameterRouteSelector
import io.ktor.server.routing.RoutingNode
import io.ktor.server.routing.RoutingRoot
import io.ktor.server.routing.getAllRoutes

/** Every endpoint the module serves, as "METHOD /path/{param}". */
fun Application.registeredEndpoints(): Set<String> = plugin(RoutingRoot).getAllRoutes().mapNotNull { route ->
    val method = (route.selector as? HttpMethodRouteSelector)?.method?.value ?: return@mapNotNull null
    "$method ${route.path()}"
}.toSet()

private fun RoutingNode.path(): String = generateSequence(this) { it.parent }.toList().asReversed()
    .mapNotNull { node ->
        when (val selector = node.selector) {
            is PathSegmentConstantRouteSelector -> selector.value
            is PathSegmentParameterRouteSelector -> "{${selector.name}}"
            else -> null
        }
    }
    .joinToString("/", prefix = "/")
