package com.sfag.shared.presentation.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val ChevronRight: ImageVector
	get() {
		if (_ChevronRight != null) {
			return _ChevronRight!!
		}
		_ChevronRight = ImageVector.Builder(
            name = "Chevron_right",
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
				moveTo(504f, 480f)
				lineTo(320f, 296f)
				lineToRelative(56f, -56f)
				lineToRelative(240f, 240f)
				lineToRelative(-240f, 240f)
				lineToRelative(-56f, -56f)
				close()
			}
		}.build()
		return _ChevronRight!!
	}

private var _ChevronRight: ImageVector? = null
