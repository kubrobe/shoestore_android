package uz.shushop.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** The few icons that are not in material-icons-core (that small library keeps the app small). */
object AppIcons {
    /** A minus sign for the quantity stepper. */
    val Remove: ImageVector by lazy {
        ImageVector.Builder(name = "Remove", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(5f, 11f); horizontalLineTo(19f); verticalLineTo(13f); horizontalLineTo(5f); close()
            }
        }.build()
    }

    /** Three lines getting shorter: the filter button. */
    val Filter: ImageVector by lazy {
        ImageVector.Builder(name = "Filter", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 6f); horizontalLineTo(21f); verticalLineTo(8f); horizontalLineTo(3f); close()
                moveTo(6f, 11f); horizontalLineTo(18f); verticalLineTo(13f); horizontalLineTo(6f); close()
                moveTo(9f, 16f); horizontalLineTo(15f); verticalLineTo(18f); horizontalLineTo(9f); close()
            }
        }.build()
    }
}
