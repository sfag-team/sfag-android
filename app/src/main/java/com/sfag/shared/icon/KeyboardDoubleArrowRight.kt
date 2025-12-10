package com.sfag.shared.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val KeyboardDoubleArrowRight: ImageVector
	get() {
		if (_KeyboardDoubleArrowRight != null) {
			return _KeyboardDoubleArrowRight!!
		}
		_KeyboardDoubleArrowRight = ImageVector.Builder(
            name = "Keyboard_double_arrow_right",
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
				moveTo(383f, 480f)
				lineTo(200f, 296f)
				lineToRelative(56f, -56f)
				lineToRelative(240f, 240f)
				lineToRelative(-240f, 240f)
				lineToRelative(-56f, -56f)
				close()
				moveToRelative(264f, 0f)
				lineTo(464f, 296f)
				lineToRelative(56f, -56f)
				lineToRelative(240f, 240f)
				lineToRelative(-240f, 240f)
				lineToRelative(-56f, -56f)
				close()
			}
		}.build()
		return _KeyboardDoubleArrowRight!!
	}

private var _KeyboardDoubleArrowRight: ImageVector? = null
