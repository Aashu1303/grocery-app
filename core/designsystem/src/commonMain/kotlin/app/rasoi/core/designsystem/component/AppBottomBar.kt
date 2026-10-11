package app.rasoi.core.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.rasoi.core.designsystem.theme.AppTheme

/** Top-level destinations; every item shows its text label as well as the icon. */
@Composable
fun AppBottomBar(
    items: List<AppBottomBarItem>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    NavigationBar(modifier = modifier, containerColor = colors.surface, contentColor = colors.onSurface) {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.key == selectedKey,
                onClick = { onSelect(item.key) },
                icon = { Icon(imageVector = item.icon, contentDescription = null) },
                label = { Text(text = item.label, style = AppTheme.type.caption) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.onPrimaryContainer,
                    selectedTextColor = colors.onSurface,
                    indicatorColor = colors.primaryContainer,
                    unselectedIconColor = colors.onSurfaceVariant,
                    unselectedTextColor = colors.onSurfaceVariant,
                ),
            )
        }
    }
}
