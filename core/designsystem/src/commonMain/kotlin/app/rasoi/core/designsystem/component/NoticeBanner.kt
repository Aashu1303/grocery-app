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
import app.rasoi.core.designsystem.theme.AppTheme

/** A notice above a screen's content, such as an import warning that needs a look before saving. */
@Composable
fun NoticeBanner(tone: NoticeTone, title: String, message: String, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val (container, content) = when (tone) {
        NoticeTone.INFO -> colors.primaryContainer to colors.onPrimaryContainer
        NoticeTone.WARNING -> colors.warningContainer to colors.onWarningContainer
        NoticeTone.DANGER -> colors.dangerContainer to colors.onDangerContainer
    }
    val icon = if (tone == NoticeTone.INFO) AppIcons.Info else AppIcons.Warning
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
            Text(text = title, style = AppTheme.type.label, color = content)
            Text(text = message, style = AppTheme.type.caption, color = content)
        }
    }
}
