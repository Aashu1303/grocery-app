package app.rasoi.core.designsystem

import app.rasoi.core.designsystem.catalog.catalogEntries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogCoverageTest {
    private val planComponents = setOf(
        "AppButton", "AppCard", "AppTopBar", "AppBottomBar", "AmountText", "ExpiryChip", "PantryItemRow",
        "BillLineRow", "SplitSelector", "QuantityStepper", "MemberAvatar", "MemberPicker", "ApprovalBanner",
        "StateViews", "AppBottomSheet", "ConfirmDialog", "AppText", "AppTextField", "ChoiceChips", "NoticeBanner",
        "FileRow", "AppScaffold", "BrandIntro", "AppPullToRefresh",
    )

    @Test
    fun everyPlannedComponentIsInTheCatalog() {
        assertEquals(planComponents, catalogEntries.map { it.component }.toSet())
    }

    @Test
    fun entryIdsAreUnique() {
        val ids = catalogEntries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun stateViewsCoverLoadingEmptyAndError() {
        val states = catalogEntries.filter { it.component == "StateViews" }.map { it.state }.toSet()
        assertTrue(states.containsAll(setOf("loading", "empty", "error")))
    }
}
