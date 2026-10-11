package app.rasoi.feature.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import app.rasoi.core.designsystem.component.BottomSheetSurface
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.model.Balance
import app.rasoi.core.model.Ledger
import app.rasoi.core.model.LedgerTransfer
import app.rasoi.core.model.Money
import app.rasoi.core.model.PeriodMonth
import app.rasoi.core.model.PeriodStatus
import app.rasoi.core.model.Role
import app.rasoi.core.model.SettlementId
import app.rasoi.core.model.SettlementStatus
import app.rasoi.core.model.Transfer
import app.rasoi.core.model.UserId
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

/** Every balances state, light and dark (design-plan 7.3: loading, empty, error, content). */
@OptIn(ExperimentalTestApi::class)
class LedgerScreenshotTest {
    private val meera = UserId("u1")
    private val aarav = UserId("u2")
    private val asha = UserId("u3")
    private val october = PeriodMonth(2026, 10)
    private val names = mapOf(meera to "Meera", aarav to "Aarav", asha to "Asha")
    private val balances = listOf(
        Balance(meera, Money(25_000)),
        Balance(aarav, Money(-10_000)),
        Balance(asha, Money(-15_000)),
    )
    private val suggested = listOf(
        LedgerTransfer(Transfer(asha, meera, Money(15_000)), payeeUpiVpa = "meera@okbank"),
        LedgerTransfer(Transfer(aarav, meera, Money(10_000)), payeeUpiVpa = "meera@okbank"),
    )
    private val open =
        Ledger(october, PeriodStatus.OPEN, balances, suggested, pendingBills = 2, months = listOf(october))
    private val closed = open.copy(
        status = PeriodStatus.LOCKED,
        pendingBills = 0,
        transfers = listOf(
            LedgerTransfer(
                Transfer(asha, meera, Money(15_000)),
                SettlementId("s1"),
                SettlementStatus.PAID,
                "meera@okbank",
            ),
            LedgerTransfer(
                Transfer(aarav, meera, Money(10_000)),
                SettlementId("s2"),
                SettlementStatus.PENDING,
                "meera@okbank",
            ),
        ),
        months = listOf(PeriodMonth(2026, 11), october),
    )

    private fun content(ledger: Ledger, me: UserId = meera, role: Role = Role.OWNER, upi: String? = "meera@okbank") =
        LedgerUiState.Content(ledger, names, me, role, upi)

    private val largeTextScreens = setOf("ledger_open_owner", "ledger_closed_payer", "ledger_upi_sheet")

    private val screens: List<Pair<String, @Composable () -> Unit>> = listOf(
        "ledger_loading" to { LedgerScreen(LedgerUiState.Loading, {}) },
        "ledger_error" to { LedgerScreen(LedgerUiState.Error(LedgerError.OFFLINE), {}) },
        "ledger_open_owner" to { LedgerScreen(content(open), {}) },
        "ledger_open_member" to { LedgerScreen(content(open, aarav, Role.MEMBER, upi = null), {}) },
        "ledger_settled" to {
            val settled = open.copy(
                balances = balances.map {
                    it.copy(net = Money.ZERO)
                },
                transfers = emptyList(),
                pendingBills = 0,
            )
            LedgerScreen(content(settled), {})
        },
        "ledger_closed_payer" to { LedgerScreen(content(closed, aarav, Role.MEMBER), {}) },
        "ledger_closed_receiver" to { LedgerScreen(content(closed), {}) },
        "ledger_closed_error" to
            { LedgerScreen(content(closed, aarav, Role.MEMBER).copy(error = LedgerError.NO_UPI_APP), {}) },
        "ledger_upi_sheet" to {
            BottomSheetSurface(title = "Your UPI address") {
                UpiContent(LedgerSheet.Upi("meera@okbank"), content(open), {})
            }
        },
        "ledger_upi_sheet_invalid" to {
            BottomSheetSurface(title = "Your UPI address") {
                UpiContent(LedgerSheet.Upi("meera bank", invalid = true), content(open), {})
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

    /** Design-plan 7.3 item 10: text at 200% must not clip; the key screens are captured at that size. */
    @Test
    fun keyScreensAtTwiceTheTextSize() {
        screens.filter { (name, _) -> name in largeTextScreens }.forEach { (name, screen) ->
            runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
                setContent {
                    AppTheme {
                        val density = Density(LocalDensity.current.density, fontScale = LARGE_FONT_SCALE)
                        CompositionLocalProvider(LocalDensity provides density) {
                            Box(
                                Modifier.testTag(TAG)
                                    .size(DpSize(AppTheme.spacing.xxl * 8, AppTheme.spacing.xxl * 14))
                                    .background(AppTheme.colors.surface),
                            ) {
                                screen()
                            }
                        }
                    }
                }
                onNodeWithTag(TAG).captureRoboImage("screenshots/${name}_large_text.png")
            }
        }
    }

    private companion object {
        const val LARGE_FONT_SCALE = 2f
        const val TAG = "screen"
        const val WIDTH = 420
        const val HEIGHT = 720
    }
}
