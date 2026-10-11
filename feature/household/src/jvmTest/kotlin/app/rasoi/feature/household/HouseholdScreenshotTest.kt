package app.rasoi.feature.household

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
import app.rasoi.core.model.LocationKind
import app.rasoi.core.model.Role
import app.rasoi.core.testing.Builders
import app.rasoi.feature.household.detail.AddLocationContent
import app.rasoi.feature.household.detail.AddLocationState
import app.rasoi.feature.household.detail.HouseholdScreen
import app.rasoi.feature.household.detail.HouseholdUiState
import app.rasoi.feature.household.detail.InviteSheetContent
import app.rasoi.feature.household.detail.InviteSheetState
import app.rasoi.feature.household.setup.HouseholdSetupScreen
import app.rasoi.feature.household.setup.HouseholdSetupUiState
import app.rasoi.feature.household.signin.SignInScreen
import app.rasoi.feature.household.signin.SignInStep
import app.rasoi.feature.household.signin.SignInUiState
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

/** Every household screen state, light and dark (design-plan 7.3: loading, empty, error, content). */
@OptIn(ExperimentalTestApi::class)
class HouseholdScreenshotTest {
    private val details = Builders.details(
        members = listOf(
            Builders.member("u1", "Meera Iyer", Role.OWNER),
            Builders.member("u2", "Aarav Shah", Role.MEMBER),
            Builders.member("u3", "Lakshmi", Role.COOK),
        ),
        locations = listOf(
            Builders.location("l1", "Freezer", LocationKind.FREEZER),
            Builders.location("l2", "Fridge", LocationKind.FRIDGE),
            Builders.location("l3", "Shelf", LocationKind.SHELF),
        ),
    )

    private val screens: List<Pair<String, @Composable () -> Unit>> = listOf(
        "signin_phone" to { SignInScreen(SignInUiState(phone = "98123 45678"), {}) },
        "signin_phone_error" to { SignInScreen(SignInUiState(phone = "123", phoneError = FieldError.INVALID), {}) },
        "signin_code" to {
            SignInScreen(SignInUiState(step = SignInStep.CODE, maskedPhone = "+91 •••••• 5678", code = "12"), {})
        },
        "signin_code_incorrect" to {
            SignInScreen(
                SignInUiState(
                    step = SignInStep.CODE,
                    maskedPhone = "+91 •••••• 5678",
                    codeError = FieldError.INCORRECT,
                ),
                {},
            )
        },
        "signin_profile" to { SignInScreen(SignInUiState(step = SignInStep.PROFILE, name = "Meera"), {}) },
        "signin_offline" to { SignInScreen(SignInUiState(error = ScreenError.OFFLINE), {}) },
        "setup_empty" to { HouseholdSetupScreen(HouseholdSetupUiState(), {}) },
        "setup_creating" to { HouseholdSetupScreen(HouseholdSetupUiState(name = "Flat 4B", isCreating = true), {}) },
        "setup_invite_used" to {
            HouseholdSetupScreen(HouseholdSetupUiState(code = "ABCD2345", error = ScreenError.INVITE_USED), {})
        },
        "household_loading" to { HouseholdScreen(HouseholdUiState.Loading, {}, {}) },
        "household_error" to { HouseholdScreen(HouseholdUiState.Error(ScreenError.OFFLINE), {}, {}) },
        "household_owner" to {
            HouseholdScreen(HouseholdUiState.Content(details, Role.OWNER, canInvite = true, canAddLocation = true), {
            }, {})
        },
        "household_cook" to {
            HouseholdScreen(HouseholdUiState.Content(details, Role.COOK, canInvite = false, canAddLocation = false), {
            }, {})
        },
        "invite_choose" to {
            BottomSheetSurface(title = "Invite someone") {
                InviteSheetContent(InviteSheetState(role = Role.COOK), canInviteOwner = true, onEvent = {})
            }
        },
        "invite_code" to {
            BottomSheetSurface(title = "Invite someone") {
                InviteSheetContent(InviteSheetState(code = "ABCD2345"), canInviteOwner = true, onEvent = {})
            }
        },
        "location_form_error" to {
            BottomSheetSurface(title = "Add storage place") {
                AddLocationContent(AddLocationState(nameError = FieldError.REQUIRED), onEvent = {})
            }
        },
        "location_form_duplicate" to {
            BottomSheetSurface(title = "Add storage place") {
                AddLocationContent(
                    AddLocationState(name = "Fridge", kind = LocationKind.FRIDGE, error = ScreenError.DUPLICATE),
                    onEvent = {},
                )
            }
        },
    )

    @Test
    fun everyScreenStateInLightAndDark() {
        screens.forEach { (name, content) ->
            listOf(false, true).forEach { dark ->
                runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
                    setContent {
                        AppTheme(darkTheme = dark) {
                            Box(
                                Modifier.testTag(TAG).size(DpSize(AppTheme.spacing.xxl * 8, AppTheme.spacing.xxl * 14)),
                            ) {
                                content()
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
