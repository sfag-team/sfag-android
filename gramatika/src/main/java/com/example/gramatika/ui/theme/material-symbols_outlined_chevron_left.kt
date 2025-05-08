package com.example.gramatika.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val Chevron_left: ImageVector
	get() {
		if (_Chevron_left != null) {
			return _Chevron_left!!
		}
		_Chevron_left = ImageVector.Builder(
            name = "Chevron_left",
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
				moveTo(560f, 720f)
				lineTo(320f, 480f)
				lineToRelative(240f, -240f)
				lineToRelative(56f, 56f)
				lineToRelative(-184f, 184f)
				lineToRelative(184f, 184f)
				close()
			}
		}.build()
		return _Chevron_left!!
	}

private var _Chevron_left: ImageVector? = null
