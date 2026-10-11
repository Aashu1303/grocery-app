package app.rasoi.buildlogic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModuleRulesTest {
    @Test
    fun allowedGraphHasNoViolations() {
        val edges = mapOf(
            ":core:domain" to setOf(":core:model"),
            ":core:data" to setOf(":core:domain", ":core:contract"),
            ":core:designsystem" to setOf(":core:model"),
            ":feature:bills" to setOf(":core:domain", ":core:designsystem", ":core:model"),
            ":server:app" to setOf(":core:domain", ":core:contract", ":server:parsing"),
            ":composeApp" to setOf(":core:data", ":feature:bills"),
        )
        assertEquals(emptyList(), ModuleRules.violations(edges))
    }

    @Test
    fun featureToFeatureIsRejected() {
        val violations = ModuleRules.violations(mapOf(":feature:bills" to setOf(":feature:pantry")))
        assertEquals(listOf(":feature:bills -> :feature:pantry: features never depend on other features"), violations)
    }

    @Test
    fun inwardLayerRulesAreEnforced() {
        val violations = ModuleRules.violations(
            mapOf(
                ":core:model" to setOf(":core:contract"),
                ":core:domain" to setOf(":core:data"),
                ":core:designsystem" to setOf(":core:domain"),
                ":feature:pantry" to setOf(":core:data"),
                ":server:jobs" to setOf(":core:designsystem"),
                ":core:data" to setOf(":core:testing"),
            ),
        )
        assertEquals(6, violations.size)
        assertTrue(violations.any { it.startsWith(":core:data -> :core:testing") })
    }
}
