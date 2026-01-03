package com.sfag.grammar.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val FileOpen: ImageVector
	get() {
		if (_FileOpen != null) {
			return _FileOpen!!
		}
		_FileOpen = ImageVector.Builder(
            name = "File_open",
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
				moveTo(240f, 880f)
				quadToRelative(-33f, 0f, -56.5f, -23.5f)
				reflectiveQuadTo(160f, 800f)
				verticalLineToRelative(-640f)
				quadToRelative(0f, -33f, 23.5f, -56.5f)
				reflectiveQuadTo(240f, 80f)
				horizontalLineToRelative(320f)
				lineToRelative(240f, 240f)
				verticalLineToRelative(240f)
				horizontalLineToRelative(-80f)
				verticalLineToRelative(-200f)
				horizontalLineTo(520f)
				verticalLineToRelative(-200f)
				horizontalLineTo(240f)
				verticalLineToRelative(640f)
				horizontalLineToRelative(360f)
				verticalLineToRelative(80f)
				close()
				moveToRelative(638f, 15f)
				lineTo(760f, 777f)
				verticalLineToRelative(89f)
				horizontalLineToRelative(-80f)
				verticalLineToRelative(-226f)
				horizontalLineToRelative(226f)
				verticalLineToRelative(80f)
				horizontalLineToRelative(-90f)
				lineToRelative(118f, 118f)
				close()
				moveToRelative(-638f, -95f)
				verticalLineToRelative(-640f)
				close()
			}
		}.build()
		return _FileOpen!!
	}

private var _FileOpen: ImageVector? = null
