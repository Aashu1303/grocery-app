package app.rasoi.core.designsystem.catalog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.rasoi.core.designsystem.brand.BrandIntroTimeline
import app.rasoi.core.designsystem.component.AmountText
import app.rasoi.core.designsystem.component.AmountTone
import app.rasoi.core.designsystem.component.AppBottomBar
import app.rasoi.core.designsystem.component.AppBottomBarItem
import app.rasoi.core.designsystem.component.AppButton
import app.rasoi.core.designsystem.component.AppButtonVariant
import app.rasoi.core.designsystem.component.AppCard
import app.rasoi.core.designsystem.component.AppKeyboard
import app.rasoi.core.designsystem.component.AppPullToRefresh
import app.rasoi.core.designsystem.component.AppScaffold
import app.rasoi.core.designsystem.component.AppText
import app.rasoi.core.designsystem.component.AppTextField
import app.rasoi.core.designsystem.component.AppTopBar
import app.rasoi.core.designsystem.component.ApprovalBanner
import app.rasoi.core.designsystem.component.AvatarSize
import app.rasoi.core.designsystem.component.BillLineRow
import app.rasoi.core.designsystem.component.BottomSheetSurface
import app.rasoi.core.designsystem.component.BrandIntro
import app.rasoi.core.designsystem.component.ChoiceChips
import app.rasoi.core.designsystem.component.ChoiceOption
import app.rasoi.core.designsystem.component.ConfirmDialogContent
import app.rasoi.core.designsystem.component.EmptyView
import app.rasoi.core.designsystem.component.ErrorView
import app.rasoi.core.designsystem.component.ExpiryChip
import app.rasoi.core.designsystem.component.FileRow
import app.rasoi.core.designsystem.component.LoadingView
import app.rasoi.core.designsystem.component.MemberAvatar
import app.rasoi.core.designsystem.component.MemberPicker
import app.rasoi.core.designsystem.component.NoticeBanner
import app.rasoi.core.designsystem.component.NoticeTone
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
    listOf(false, true).forEach { refreshing ->
        add(
            CatalogEntry("AppPullToRefresh", if (refreshing) "refreshing" else "idle") {
                AppPullToRefresh(refreshing = refreshing, onRefresh = {}) {
                    LazyColumn { items(3) { Text(S.ITEM, style = AppTheme.type.body) } }
                }
            },
        )
    }
    add(
        CatalogEntry("AppBottomBar", "selected") {
            AppBottomBar(bottomBarItems, selectedKey = "pantry", onSelect = {})
        },
    )
    listOf(true, false).forEach { visible ->
        add(CatalogEntry("AppScaffold", if (visible) "bars" else "no_bars") { ScaffoldSample(barsVisible = visible) })
    }

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

    add(CatalogEntry("FileRow", "removable") { FileRow(S.FILE_NAME, S.FILE_DETAIL, S.REMOVE, onRemove = noop) })
    add(CatalogEntry("FileRow", "fixed") { FileRow(S.FILE_NAME, S.FILE_DETAIL, S.REMOVE) })

    NoticeTone.entries.forEach { tone ->
        add(
            CatalogEntry("NoticeBanner", tone.name.lowercase()) {
                NoticeBanner(tone, S.NOTICE_TITLE, S.NOTICE_MESSAGE)
            },
        )
    }

    add(CatalogEntry("AppText", "title") { AppText(S.TITLE, style = AppTheme.type.title, heading = true) })
    add(CatalogEntry("AppText", "body") { AppText(S.MESSAGE) })
    add(
        CatalogEntry("AppTextField", "empty") {
            AppTextField("", {}, label = S.PHONE_LABEL, keyboard = AppKeyboard.PHONE)
        },
    )
    add(CatalogEntry("AppTextField", "filled") { AppTextField(S.HOUSEHOLD, {}, label = S.NAME_LABEL) })
    add(
        CatalogEntry("AppTextField", "error") {
            AppTextField("123", {}, label = S.PHONE_LABEL, error = S.PHONE_ERROR, keyboard = AppKeyboard.PHONE)
        },
    )
    add(
        CatalogEntry("AppTextField", "disabled") {
            AppTextField(S.HOUSEHOLD, {}, label = S.NAME_LABEL, enabled = false)
        },
    )
    add(
        CatalogEntry("ChoiceChips", "selected") {
            ChoiceChips(S.roles.map { ChoiceOption(it, it) }, selected = S.roles[1], onSelect = {})
        },
    )
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

    // Key frames of the launch intro (ADR 0027): the launch-screen match, the zoom peak, the leaf landing, the end.
    listOf(
        "launch" to 0,
        "zoom" to BrandIntroTimeline.ZOOM_END,
        "tadka" to BrandIntroTimeline.LAND_END,
        "lockup" to BrandIntroTimeline.DURATION_MILLIS,
    ).forEach { (state, millis) ->
        add(
            CatalogEntry("BrandIntro", state) {
                BrandIntro(millis, Modifier.fillMaxWidth().height(AppTheme.spacing.xxl * BRAND_INTRO_HEIGHT_UNITS))
            },
        )
    }
}

private const val BRAND_INTRO_HEIGHT_UNITS = 8

@Composable
private fun ScaffoldSample(barsVisible: Boolean) {
    Box(Modifier.height(AppTheme.spacing.xxl * SCAFFOLD_HEIGHT_UNITS)) {
        AppScaffold(
            barsVisible = barsVisible,
            topBar = { AppTopBar(title = S.TITLE) },
            bottomBar = { AppBottomBar(bottomBarItems, selectedKey = "pantry", onSelect = {}) },
        ) { padding -> AppText(S.ITEM, Modifier.padding(padding).padding(AppTheme.spacing.md)) }
    }
}

private const val SCAFFOLD_HEIGHT_UNITS = 6

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
