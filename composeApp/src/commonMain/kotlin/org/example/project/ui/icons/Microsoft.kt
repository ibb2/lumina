package org.example.project.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val Microsoft: ImageVector
	get() {
		if (_Microsoft != null) {
			return _Microsoft!!
		}
		_Microsoft = ImageVector.Builder(
            name = "Microsoft",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ).apply {
			path(
    			fill = SolidColor(Color(0xFF000000)),
    			fillAlpha = 1.0f,
    			stroke = null,
    			strokeAlpha = 1.0f,
    			strokeLineWidth = 1.0f,
    			strokeLineCap = StrokeCap.Butt,
    			strokeLineJoin = StrokeJoin.Miter,
    			strokeLineMiter = 1.0f,
    			pathFillType = PathFillType.NonZero
			) {
				moveTo(7.462f, 0f)
				horizontalLineTo(0f)
				verticalLineToRelative(7.19f)
				horizontalLineToRelative(7.462f)
				close()
				moveTo(16f, 0f)
				horizontalLineTo(8.538f)
				verticalLineToRelative(7.19f)
				horizontalLineTo(16f)
				close()
				moveTo(7.462f, 8.211f)
				horizontalLineTo(0f)
				verticalLineTo(16f)
				horizontalLineToRelative(7.462f)
				close()
				moveToRelative(8.538f, 0f)
				horizontalLineTo(8.538f)
				verticalLineTo(16f)
				horizontalLineTo(16f)
				close()
			}
		}.build()
		return _Microsoft!!
	}

private var _Microsoft: ImageVector? = null
