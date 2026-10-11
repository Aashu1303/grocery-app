package app.rasoi.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import app.rasoi.core.designsystem.theme.AppTheme

/** Text in a design-system style. Headings are marked for screen readers. */
@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = AppTheme.type.body,
    color: Color = AppTheme.colors.onSurface,
    textAlign: TextAlign? = null,
    heading: Boolean = false,
) {
    Text(
        text = text,
        modifier = if (heading) modifier.semantics { heading() } else modifier,
        style = style,
        color = color,
        textAlign = textAlign,
    )
}
