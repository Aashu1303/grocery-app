package app.rasoi.buildlogic

/** Dependency rules from design-plan section 3: features are independent and layers point inward. */
object ModuleRules {
    private class Rule(val reason: String, val breaks: (from: String, to: String) -> Boolean)

    private fun String.isFeature() = startsWith(":feature:")

    private val serverForbidden = listOf(":feature:", ":core:data", ":core:designsystem", ":composeApp")

    private val rules = listOf(
        Rule("test fixtures are for test source sets only") { _, to -> to == ":core:testing" },
        Rule("features never depend on other features") { from, to -> from.isFeature() && to.isFeature() },
        Rule("features see repositories via core:domain only") { from, to ->
            from.isFeature() && to in setOf(":core:data", ":composeApp")
        },
        Rule("must not depend on other modules") { from, _ -> from == ":core:model" || from == ":core:contract" },
        Rule("domain depends on core:model only") { from, to -> from == ":core:domain" && to != ":core:model" },
        Rule("design system depends on core:model only") { from, to ->
            from == ":core:designsystem" && to != ":core:model"
        },
        Rule("server code is client-independent") { from, to ->
            from.startsWith(":server:") && serverForbidden.any { to.startsWith(it) }
        },
    )

    fun violations(edges: Map<String, Set<String>>): List<String> = edges.toSortedMap().flatMap { (from, targets) ->
        targets.sorted().mapNotNull { to ->
            rules.firstOrNull { it.breaks(from, to) }?.let { "$from -> $to: ${it.reason}" }
        }
    }
}
