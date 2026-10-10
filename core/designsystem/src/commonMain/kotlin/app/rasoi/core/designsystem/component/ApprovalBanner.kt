package app.rasoi.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import app.rasoi.core.designsystem.icon.AppIcons
import app.rasoi.core.designsystem.resources.Res
import app.rasoi.core.designsystem.resources.ds_approval_approved
import app.rasoi.core.designsystem.resources.ds_approval_changes_requested
import app.rasoi.core.designsystem.resources.ds_approval_pending
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.model.ApprovalState
import org.jetbrains.compose.resources.stringResource

/** Bill approval status with an icon, a title and a detail line such as "2 of 3 approved". */
@Composable
fun ApprovalBanner(state: ApprovalState, message: String, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val (container, content) = when (state) {
        ApprovalState.PENDING -> colors.warningContainer to colors.onWarningContainer
        ApprovalState.APPROVED -> colors.successContainer to colors.onSuccessContainer
        ApprovalState.CHANGES_REQUESTED -> colors.dangerContainer to colors.onDangerContainer
    }
    val (icon, title) = when (state) {
        ApprovalState.PENDING -> AppIcons.Clock to Res.string.ds_approval_pending
        ApprovalState.APPROVED -> AppIcons.Check to Res.string.ds_approval_approved
        ApprovalState.CHANGES_REQUESTED -> AppIcons.Warning to Res.string.ds_approval_changes_requested
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(container, AppTheme.shapes.card)
            .padding(AppTheme.spacing.md)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = content)
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs)) {
            Text(text = stringResource(title), style = AppTheme.type.label, color = content)
            Text(text = message, style = AppTheme.type.caption, color = content)
        }
    }
}
