package app.rasoi.core.designsystem.catalog

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import app.rasoi.core.designsystem.component.AmountText
import app.rasoi.core.designsystem.component.AmountTone
import app.rasoi.core.designsystem.component.AppBottomBar
import app.rasoi.core.designsystem.component.AppBottomBarItem
import app.rasoi.core.designsystem.component.AppButton
import app.rasoi.core.designsystem.component.AppButtonVariant
import app.rasoi.core.designsystem.component.AppCard
import app.rasoi.core.designsystem.component.AppTopBar
import app.rasoi.core.designsystem.component.ApprovalBanner
import app.rasoi.core.designsystem.component.AvatarSize
import app.rasoi.core.designsystem.component.BillLineRow
import app.rasoi.core.designsystem.component.BottomSheetSurface
import app.rasoi.core.designsystem.component.ConfirmDialogContent
import app.rasoi.core.designsystem.component.EmptyView
import app.rasoi.core.designsystem.component.ErrorView
import app.rasoi.core.designsystem.component.ExpiryChip
import app.rasoi.core.designsystem.component.LoadingView
import app.rasoi.core.designsystem.component.MemberAvatar
import app.rasoi.core.designsystem.component.MemberPicker
import app.rasoi.core.designsystem.component.PantryItemRow
import app.rasoi.core.designsystem.component.QuantityStepper
import app.rasoi.core.designsystem.component.SplitSelector
import app.rasoi.core.designsystem.icon.AppIcons
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.model.ApprovalState
import app.rasoi.core.model.Freshness
import app.rasoi.core.model.Money
import app.rasoi.core.model.SplitMode
import app.rasoi.core.designsystem.catalog.CatalogSamples as S

private val noop: () -> Unit = {}

/** Every design-system component in every state (design-plan 7.2). */
val catalogEntries: List<CatalogEntry> = buildList {
    AppButtonVariant.entries.forEach { variant ->
        add(CatalogEntry("AppButton", variant.name.lowercase()) { AppButton(S.APPROVE, noop, variant = variant) })
    }
    add(CatalogEntry("AppButton", "disabled") { AppButton(S.APPROVE, noop, enabled = false) })
    add(CatalogEntry("AppButton", "loading") { AppButton(S.APPROVE, noop, loading = true) })

    add(CatalogEntry("AppCard", "static") { AppCard { Text(S.ITEM, style = AppTheme.type.body) } })
    add(CatalogEntry("AppCard", "clickable") { AppCard(onClick = noop) { Text(S.ITEM, style = AppTheme.type.body) } })

    add(CatalogEntry("AppTopBar", "title") { AppTopBar(title = S.TITLE) })
    add(CatalogEntry("AppTopBar", "back") { AppTopBar(title = S.TITLE, onBack = noop) })
    add(
        CatalogEntry("AppBottomBar", "selected") {
            AppBottomBar(bottomBarItems, selectedKey = "pantry", onSelect = {})
        },
    )

    add(CatalogEntry("AmountText", "neutral") { AmountText(Money(46_901)) })
    add(CatalogEntry("AmountText", "owed_to_you") { AmountText(Money(12_345_600), tone = AmountTone.OWED_TO_YOU) })
    add(CatalogEntry("AmountText", "you_owe") { AmountText(Money(-8_050), tone = AmountTone.YOU_OWE) })

    Freshness.entries.forEach { freshness ->
        add(CatalogEntry("ExpiryChip", freshness.name.lowercase()) { ExpiryChip(freshness, freshness.sampleLabel()) })
        add(
            CatalogEntry("PantryItemRow", freshness.name.lowercase()) {
                PantryItemRow(S.ITEM, S.QUANTITY, S.LOCATION, freshness, freshness.sampleLabel(), onClick = noop)
            },
        )
    }

    add(CatalogEntry("BillLineRow", "food") { BillLineRow(S.VEGETABLE, Money(1_900), S.SPLIT_SUMMARY) })
    add(CatalogEntry("BillLineRow", "tagged") { BillLineRow(S.ITEM, Money(12_900), S.SPLIT_SUMMARY, tag = S.NON_FOOD) })

    SplitMode.entries.forEach { mode ->
        add(CatalogEntry("SplitSelector", mode.name.lowercase()) { SplitSelector(selected = mode, onSelect = {}) })
    }

    add(CatalogEntry("QuantityStepper", "enabled") { QuantityStepper("2", S.ITEM, noop, noop) })
    add(
        CatalogEntry("QuantityStepper", "at_minimum") {
            QuantityStepper("0", S.ITEM, noop, noop, canDecrement = false)
        },
    )

    AvatarSize.entries.forEach { size ->
        add(CatalogEntry("MemberAvatar", size.name.lowercase()) { MemberAvatar(S.members.first().name, size = size) })
    }
    add(CatalogEntry("MemberPicker", "none") { MemberPicker(S.members, emptySet(), onToggle = {}) })
    add(CatalogEntry("MemberPicker", "some") { MemberPicker(S.members, setOf("u1", "u3"), onToggle = {}) })

    ApprovalState.entries.forEach { state ->
        add(CatalogEntry("ApprovalBanner", state.name.lowercase()) { ApprovalBanner(state, "2 of 3 approved") })
    }

    add(CatalogEntry("StateViews", "loading") { LoadingView() })
    add(CatalogEntry("StateViews", "empty") { EmptyView(S.TITLE, S.MESSAGE, actionLabel = S.ACTION, onAction = noop) })
    add(CatalogEntry("StateViews", "error") { ErrorView(S.ERROR, onRetry = noop) })

    add(
        CatalogEntry("AppBottomSheet", "content") {
            BottomSheetSurface(title = S.LOCATION) { Column { Text(S.ITEM, style = AppTheme.type.body) } }
        },
    )
    add(
        CatalogEntry("ConfirmDialog", "destructive") {
            ConfirmDialogContent(S.DELETE, S.MESSAGE, S.DELETE, onConfirm = noop, onDismiss = noop)
        },
    )
    add(
        CatalogEntry("ConfirmDialog", "neutral") {
            ConfirmDialogContent(S.APPROVE, S.MESSAGE, S.APPROVE, noop, noop, destructive = false)
        },
    )
}

private val bottomBarItems = listOf(
    AppBottomBarItem("kitchen", "Kitchen", AppIcons.Kitchen),
    AppBottomBarItem("pantry", "Pantry", AppIcons.Pantry),
    AppBottomBarItem("bills", "Bills", AppIcons.Bills),
    AppBottomBarItem("balances", "Balances", AppIcons.Balances),
)

private fun Freshness.sampleLabel(): String = when (this) {
    Freshness.FRESH -> "6 days left"
    Freshness.USE_SOON -> "2 days left"
    Freshness.EXPIRED -> "Expired 12 Oct"
}
