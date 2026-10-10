package app.rasoi.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Line icons on a 24 dp grid with a 2 dp stroke. Callers tint them, so the stroke colour here is irrelevant. */
object AppIcons {
    val Kitchen: ImageVector by lazy { icon("Kitchen", "M4 10H20L19 19H5Z M2 10H22 M9 7H15 M12 4V7") }
    val Pantry: ImageVector by lazy { icon("Pantry", "M5 3H19V21H5Z M5 9H19 M5 15H19") }
    val Bills: ImageVector by lazy { icon("Bills", "M6 3H18V21L15 19L12 21L9 19L6 21Z M9 8H15 M9 12H15") }
    val Balances: ImageVector by lazy { icon("Balances", "M4 8H20 M16 4L20 8L16 12 M20 16H4 M8 12L4 16L8 20") }
    val Add: ImageVector by lazy { icon("Add", "M12 5V19 M5 12H19") }
    val Remove: ImageVector by lazy { icon("Remove", "M5 12H19") }
    val Check: ImageVector by lazy { icon("Check", "M5 12L10 17L19 7") }
    val Close: ImageVector by lazy { icon("Close", "M6 6L18 18 M18 6L6 18") }
    val Back: ImageVector by lazy { icon("Back", "M15 5L8 12L15 19") }
    val Warning: ImageVector by lazy { icon("Warning", "M12 3L22 20H2Z M12 9V14 M12 17V17.5") }
    val Clock: ImageVector by lazy { icon("Clock", "M12 3A9 9 0 1 1 12 21A9 9 0 1 1 12 3Z M12 7V12L15 14") }

    private fun icon(name: String, pathData: String): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(pathData),
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ).build()
}
