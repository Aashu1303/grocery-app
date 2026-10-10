package app.rasoi.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import app.rasoi.core.designsystem.resources.Res
import app.rasoi.core.designsystem.resources.ds_cancel
import app.rasoi.core.designsystem.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

/** Confirmation before a destructive or irreversible action; the confirm label names the action. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
) {
    Dialog(onDismissRequest = onDismiss) {
        ConfirmDialogContent(title, message, confirmLabel, onConfirm, onDismiss, destructive = destructive)
    }
}

/** The dialog card on its own, for the catalog and screenshot tests. */
@Composable
fun ConfirmDialogContent(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = true,
) {
    Surface(
        modifier = modifier,
        shape = AppTheme.shapes.card,
        color = AppTheme.colors.surface,
        contentColor = AppTheme.colors.onSurface,
        shadowElevation = AppTheme.shapes.elevationHigh,
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            Text(text = title, style = AppTheme.type.title, modifier = Modifier.semantics { heading() })
            Text(text = message, style = AppTheme.type.body, color = AppTheme.colors.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs, Alignment.End),
            ) {
                AppButton(
                    text = stringResource(Res.string.ds_cancel),
                    onClick = onDismiss,
                    variant = AppButtonVariant.TEXT,
                )
                AppButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                    variant = if (destructive) AppButtonVariant.DESTRUCTIVE else AppButtonVariant.PRIMARY,
                )
            }
        }
    }
}
