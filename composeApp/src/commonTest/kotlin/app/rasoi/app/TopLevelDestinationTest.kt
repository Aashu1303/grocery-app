package app.rasoi.app

import kotlin.test.Test
import kotlin.test.assertEquals

class TopLevelDestinationTest {
    @Test
    fun bottomBarOrderMatchesThePlan() {
        assertEquals(
            listOf(Route.Kitchen, Route.Pantry, Route.Bills, Route.Balances),
            TopLevelDestination.entries.map { it.route },
        )
    }
}
