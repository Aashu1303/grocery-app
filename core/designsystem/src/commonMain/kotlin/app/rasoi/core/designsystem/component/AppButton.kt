package app.rasoi.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.rasoi.core.designsystem.resources.Res
import app.rasoi.core.designsystem.resources.ds_loading
import app.rasoi.core.designsystem.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

/** The only button in the app. Labels are verbs ("Approve split"); [loading] disables it and shows progress. */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.PRIMARY,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val sized = modifier.heightIn(min = AppTheme.spacing.minTouchTarget)
    val active = enabled && !loading
    val content: @Composable () -> Unit = { ButtonContent(text, loading) }
    when (variant) {
        AppButtonVariant.PRIMARY -> Button(onClick, sized, active, colors = filledColors(destructive = false)) {
            content()
        }
        AppButtonVariant.DESTRUCTIVE -> Button(onClick, sized, active, colors = filledColors(destructive = true)) {
            content()
        }
        AppButtonVariant.SECONDARY -> OutlinedButton(
            onClick = onClick,
            modifier = sized,
            enabled = active,
            border = BorderStroke(AppTheme.shapes.elevationLow, AppTheme.colors.outline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.primary),
        ) { content() }
        AppButtonVariant.TEXT -> TextButton(
            onClick = onClick,
            modifier = sized,
            enabled = active,
            colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.colors.primary),
        ) { content() }
    }
}

@Composable
private fun ButtonContent(text: String, loading: Boolean) {
    if (loading) {
        val description = stringResource(Res.string.ds_loading)
        CircularProgressIndicator(
            modifier = Modifier.size(AppTheme.spacing.md).semantics { contentDescription = description },
            strokeWidth = AppTheme.shapes.elevationHigh / 2,
            color = AppTheme.colors.onSurfaceVariant,
        )
    } else {
        Text(text = text, style = AppTheme.type.label)
    }
}

@Composable
private fun filledColors(destructive: Boolean): ButtonColors {
    val colors = AppTheme.colors
    return ButtonDefaults.buttonColors(
        containerColor = if (destructive) colors.danger else colors.primary,
        contentColor = if (destructive) colors.onDanger else colors.onPrimary,
        disabledContainerColor = colors.surfaceVariant,
        disabledContentColor = colors.onSurfaceVariant,
    )
}
