import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val Switch_access_shortcut: ImageVector
    get() {
        if (_Switch_access_shortcut != null) {
            return _Switch_access_shortcut!!
        }
        _Switch_access_shortcut = ImageVector.Builder(
            name = "Switch_access_shortcut",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(600f, 880f)
                quadToRelative(-127f, -48f, -203.5f, -158f)
                reflectiveQuadTo(320f, 476f)
                quadToRelative(0f, -91f, 36f, -172.5f)
                reflectiveQuadTo(458f, 160f)
                horizontalLineTo(320f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(280f)
                verticalLineToRelative(280f)
                horizontalLineToRelative(-80f)
                verticalLineToRelative(-148f)
                quadToRelative(-57f, 51f, -88.5f, 119.5f)
                reflectiveQuadTo(400f, 476f)
                quadToRelative(0f, 102f, 54f, 187.5f)
                reflectiveQuadTo(600f, 793f)
                close()
            }
        }.build()
        return _Switch_access_shortcut!!
    }

private var _Switch_access_shortcut: ImageVector? = null

