package app.rasoi.feature.bills

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.DpSize
import app.rasoi.core.designsystem.component.BottomSheetSurface
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.model.Approval
import app.rasoi.core.model.ApprovalState
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillId
import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.BillSummary
import app.rasoi.core.model.ChargeDistribution
import app.rasoi.core.model.ChargeKind
import app.rasoi.core.model.Money
import app.rasoi.core.model.Platform
import app.rasoi.core.model.Role
import app.rasoi.core.model.SplitMode
import app.rasoi.core.model.UserId
import app.rasoi.core.testing.Builders
import app.rasoi.feature.bills.detail.BillDetailScreen
import app.rasoi.feature.bills.detail.BillDetailUiState
import app.rasoi.feature.bills.detail.BillSheet
import app.rasoi.feature.bills.detail.ChargeContent
import app.rasoi.feature.bills.detail.PayerContent
import app.rasoi.feature.bills.detail.RequestChangesContent
import app.rasoi.feature.bills.detail.SplitContent
import app.rasoi.feature.bills.detail.SplitError
import app.rasoi.feature.bills.list.BillsScreen
import app.rasoi.feature.bills.list.BillsUiState
import app.rasoi.feature.bills.list.NewBillContent
import app.rasoi.feature.bills.list.NewBillForm
import io.github.takahirom.roborazzi.captureRoboImage
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.time.Instant

/** Every bills state, light and dark (design-plan 7.3: loading, empty, error, content). */
@OptIn(ExperimentalTestApi::class)
class BillsScreenshotTest {
    private val meera = UserId("u1")
    private val aarav = UserId("u2")
    private val members = listOf(
        Builders.member("u1", "Meera"),
        Builders.member("u2", "Aarav", Role.MEMBER),
        Builders.member("u3", "Lakshmi", Role.COOK),
    )
    private val names = members.associate { it.userId to it.displayName }
    private val rice = Builders.line("a", "Basmati rice 5 kg", 30_000, units = 3).copy(isFood = true)
    private val milk = Builders.line("b", "Amul Taaza milk", 10_000).copy(isFood = true)
    private val soap = Builders.line("c", "Dishwash liquid", 4_900)
    private val delivery = Builders.charge("c1", ChargeKind.DELIVERY, "Delivery fee", 2_500)
    private val discount = Builders.charge("c2", ChargeKind.DISCOUNT, "Coupon", -1_000)
        .copy(distribution = ChargeDistribution.EQUAL)
    private val allocations =
        Builders.split(rice, SplitMode.EQUAL, "u1", "u2") + Builders.split(milk, SplitMode.ONE, "u2") +
            Builders.split(soap, SplitMode.EQUAL, "u1", "u2")
    private val pendingApprovals = listOf(Approval(aarav, ApprovalState.PENDING))

    private fun bill(
        status: BillStatus = BillStatus.DRAFT,
        split: Boolean = true,
        approvals: List<Approval> = emptyList(),
        total: Money? = null,
    ): Bill {
        val base = Builders.bill(
            lines = listOf(rice, milk, soap),
            charges = listOf(delivery, discount),
            status = status,
            approvals = approvals,
            allocations = if (split) allocations else emptyList(),
        ).copy(submittedAt = Instant.parse("2026-10-05T10:00:00Z"), invoiceNumber = "INV-77")
        return if (total == null) base else base.copy(total = total)
    }

    private fun detail(
        bill: Bill,
        me: UserId = meera,
        role: Role = Role.OWNER,
        sheet: BillSheet? = null,
        error: BillsError? = null,
    ) = BillDetailUiState.Content(bill, members, me, role, sheet, error = error)

    private val summaries = listOf(
        BillSummary(
            BillId(
                "b1",
            ),
            Platform.ZEPTO, null, LocalDate(2026, 10, 10), meera, meera, BillStatus.DRAFT, Money(86_700), 11,
        ),
        BillSummary(
            BillId("b2"), Platform.MANUAL, "Gas cylinder", LocalDate(2026, 10, 5), aarav, aarav, BillStatus.PENDING,
            Money(95_000), 1,
        ),
        BillSummary(
            BillId(
                "b3",
            ),
            Platform.BLINKIT, null, LocalDate(2026, 10, 2), meera, meera, BillStatus.APPROVED, Money(25_800), 4,
        ),
        BillSummary(
            BillId("b4"), Platform.INSTAMART, null, LocalDate(2026, 9, 28), aarav, aarav, BillStatus.CHANGES_REQUESTED,
            Money(46_901), 7,
        ),
    )
    private val list = BillsUiState.Content(summaries, names, canEdit = true)

    private fun split(
        mode: SplitMode,
        error: SplitError? = null,
        units: Map<UserId, Int> = mapOf(meera to 1, aarav to 1),
    ) = BillSheet.Split(BillLineId("a"), mode, units, error)

    private val screens: List<Pair<String, @Composable () -> Unit>> = listOf(
        "bills_loading" to { BillsScreen(BillsUiState.Loading, {}) },
        "bills_error" to { BillsScreen(BillsUiState.Error(BillsError.OFFLINE), {}) },
        "bills_empty" to { BillsScreen(list.copy(bills = emptyList()), {}) },
        "bills_list" to { BillsScreen(list, {}) },
        "bills_new_form" to {
            BottomSheetSurface(title = "Add a bill") {
                NewBillContent(NewBillForm(title = "Gas cylinder", amountText = "950.00"), onEvent = {})
            }
        },
        "bills_new_form_errors" to {
            BottomSheetSurface(title = "Add a bill") {
                val form =
                    NewBillForm(amountText = "lots", titleError = FieldError.REQUIRED, amountError = FieldError.INVALID)
                NewBillContent(form, onEvent = {})
            }
        },
        "detail_loading" to { BillDetailScreen(BillDetailUiState.Loading, {}, onBack = {}) },
        "detail_error" to { BillDetailScreen(BillDetailUiState.Error(BillsError.NOT_FOUND), {}, onBack = {}) },
        "detail_draft_unsplit" to { BillDetailScreen(detail(bill(split = false)), {}, onBack = {}) },
        "detail_draft_split" to { BillDetailScreen(detail(bill()), {}, onBack = {}) },
        "detail_mismatch" to { BillDetailScreen(detail(bill(total = Money(50_000))), {}, onBack = {}) },
        "detail_not_split_error" to {
            BillDetailScreen(detail(bill(split = false), error = BillsError.NOT_SPLIT), {}, onBack = {})
        },
        "detail_pending_uploader" to {
            BillDetailScreen(detail(bill(BillStatus.PENDING, approvals = pendingApprovals)), {}, onBack = {})
        },
        "detail_pending_approver" to {
            val state = detail(bill(BillStatus.PENDING, approvals = pendingApprovals), aarav, Role.MEMBER)
            BillDetailScreen(state, {}, onBack = {})
        },
        "detail_changes_requested" to {
            val approvals = listOf(Approval(aarav, ApprovalState.CHANGES_REQUESTED, "The milk was mine only"))
            BillDetailScreen(detail(bill(BillStatus.CHANGES_REQUESTED, approvals = approvals)), {}, onBack = {})
        },
        "detail_approved_automatically" to {
            val approvals = listOf(Approval(aarav, ApprovalState.APPROVED, automatic = true))
            BillDetailScreen(detail(bill(BillStatus.APPROVED, approvals = approvals)), {}, onBack = {})
        },
        "detail_locked" to {
            val approvals = listOf(Approval(aarav, ApprovalState.APPROVED))
            BillDetailScreen(detail(bill(BillStatus.LOCKED, approvals = approvals)), {}, onBack = {})
        },
        "sheet_split_equal" to {
            BottomSheetSurface(title = "Split Basmati rice 5 kg") {
                SplitContent(split(SplitMode.EQUAL), rice, detail(bill()), onEvent = {})
            }
        },
        "sheet_split_quantity_error" to {
            BottomSheetSurface(title = "Split Basmati rice 5 kg") {
                val sheet = split(SplitMode.QUANTITY, SplitError.UNITS, mapOf(meera to 1, aarav to 1))
                SplitContent(sheet, rice, detail(bill()), onEvent = {})
            }
        },
        "sheet_payer" to { BottomSheetSurface(title = "Who paid?") { PayerContent(detail(bill()), onEvent = {}) } },
        "sheet_charge" to {
            BottomSheetSurface(title = "How is Coupon shared?") {
                ChargeContent(ChargeDistribution.EQUAL, detail(bill()), onEvent = {})
            }
        },
        "sheet_request_changes" to {
            BottomSheetSurface(title = "Ask for changes") {
                val sheet = BillSheet.RequestChanges("", FieldError.REQUIRED)
                RequestChangesContent(sheet, detail(bill(BillStatus.PENDING), aarav, Role.MEMBER), onEvent = {})
            }
        },
        "sheet_split_save_failed" to {
            BottomSheetSurface(title = "Split Basmati rice 5 kg") {
                SplitContent(split(SplitMode.EQUAL), rice, detail(bill(), error = BillsError.OFFLINE), onEvent = {})
            }
        },
    )

    @Test
    fun everyScreenStateInLightAndDark() {
        screens.forEach { (name, screen) ->
            listOf(false, true).forEach { dark ->
                runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
                    setContent {
                        AppTheme(darkTheme = dark) {
                            Box(
                                Modifier.testTag(TAG)
                                    .size(DpSize(AppTheme.spacing.xxl * 8, AppTheme.spacing.xxl * 14))
                                    .background(AppTheme.colors.surface),
                            ) {
                                screen()
                            }
                        }
                    }
                    onNodeWithTag(TAG).captureRoboImage("screenshots/${name}_${if (dark) "dark" else "light"}.png")
                }
            }
        }
    }

    private companion object {
        const val TAG = "screen"
        const val WIDTH = 420
        const val HEIGHT = 720
    }
}
