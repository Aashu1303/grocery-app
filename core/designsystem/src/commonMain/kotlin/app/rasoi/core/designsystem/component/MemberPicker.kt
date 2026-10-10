package app.rasoi.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import app.rasoi.core.designsystem.resources.Res
import app.rasoi.core.designsystem.resources.ds_not_selected
import app.rasoi.core.designsystem.resources.ds_selected
import app.rasoi.core.designsystem.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

/** Selects household members, for example who shares a line. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemberPicker(
    members: List<MemberOption>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedText = stringResource(Res.string.ds_selected)
    val notSelectedText = stringResource(Res.string.ds_not_selected)
    val colors = AppTheme.colors
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        members.forEach { member ->
            val selected = member.id in selectedIds
            FilterChip(
                selected = selected,
                onClick = { onToggle(member.id) },
                label = { Text(text = member.name, style = AppTheme.type.label) },
                leadingIcon = { MemberAvatar(name = member.name, size = AvatarSize.SMALL) },
                modifier = Modifier
                    .heightIn(min = AppTheme.spacing.minTouchTarget)
                    .semantics { stateDescription = if (selected) selectedText else notSelectedText },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = colors.surface,
                    labelColor = colors.onSurface,
                    selectedContainerColor = colors.primaryContainer,
                    selectedLabelColor = colors.onPrimaryContainer,
                ),
            )
        }
    }
}
