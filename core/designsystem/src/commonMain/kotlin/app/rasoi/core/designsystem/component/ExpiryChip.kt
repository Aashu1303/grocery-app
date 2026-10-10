package app.rasoi.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import app.rasoi.core.designsystem.icon.AppIcons
import app.rasoi.core.designsystem.resources.Res
import app.rasoi.core.designsystem.resources.ds_freshness_expired
import app.rasoi.core.designsystem.resources.ds_freshness_fresh
import app.rasoi.core.designsystem.resources.ds_freshness_use_soon
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.model.Freshness
import org.jetbrains.compose.resources.stringResource

/** Freshness badge. Always shows text such as "2 days left", never colour alone. */
@Composable
fun ExpiryChip(freshness: Freshness, label: String, modifier: Modifier = Modifier) {
    val (container, content) = AppTheme.colors.freshnessContainer(freshness)
    val status = stringResource(
        when (freshness) {
            Freshness.FRESH -> Res.string.ds_freshness_fresh
            Freshness.USE_SOON -> Res.string.ds_freshness_use_soon
            Freshness.EXPIRED -> Res.string.ds_freshness_expired
        },
    )
    Row(
        modifier = modifier
            .clearAndSetSemantics { contentDescription = "$status, $label" }
            .background(container, AppTheme.shapes.pill)
            .padding(horizontal = AppTheme.spacing.xs, vertical = AppTheme.spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (freshness == Freshness.FRESH) AppIcons.Check else AppIcons.Clock,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(AppTheme.spacing.md),
        )
        Text(text = label, style = AppTheme.type.caption, color = content)
    }
}
