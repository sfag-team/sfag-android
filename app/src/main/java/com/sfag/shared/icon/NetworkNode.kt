package com.sfag.shared.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val NetworkNode: ImageVector
    get() {
        if (_NetworkNode != null) {
            return _NetworkNode!!
        }
        _NetworkNode = ImageVector.Builder(
            name = "Network_node",
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
                moveTo(220f, 880f)
                quadToRelative(-58f, 0f, -99f, -41f)
                reflectiveQuadToRelative(-41f, -99f)
                reflectiveQuadToRelative(41f, -99f)
                reflectiveQuadToRelative(99f, -41f)
                quadToRelative(18f, 0f, 35f, 4.5f)
                reflectiveQuadToRelative(32f, 12.5f)
                lineToRelative(153f, -153f)
                verticalLineToRelative(-110f)
                quadToRelative(-44f, -13f, -72f, -49.5f)
                reflectiveQuadTo(340f, 220f)
                quadToRelative(0f, -58f, 41f, -99f)
                reflectiveQuadToRelative(99f, -41f)
                reflectiveQuadToRelative(99f, 41f)
                reflectiveQuadToRelative(41f, 99f)
                quadToRelative(0f, 48f, -28f, 84.5f)
                reflectiveQuadTo(520f, 354f)
                verticalLineToRelative(110f)
                lineToRelative(154f, 153f)
                quadToRelative(15f, -8f, 31.5f, -12.5f)
                reflectiveQuadTo(740f, 600f)
                quadToRelative(58f, 0f, 99f, 41f)
                reflectiveQuadToRelative(41f, 99f)
                reflectiveQuadToRelative(-41f, 99f)
                reflectiveQuadToRelative(-99f, 41f)
                reflectiveQuadToRelative(-99f, -41f)
                reflectiveQuadToRelative(-41f, -99f)
                quadToRelative(0f, -18f, 4.5f, -35f)
                reflectiveQuadToRelative(12.5f, -32f)
                lineTo(480f, 536f)
                lineTo(343f, 673f)
                quadToRelative(8f, 15f, 12.5f, 32f)
                reflectiveQuadToRelative(4.5f, 35f)
                quadToRelative(0f, 58f, -41f, 99f)
                reflectiveQuadToRelative(-99f, 41f)
                moveToRelative(520f, -80f)
                quadToRelative(25f, 0f, 42.5f, -17.5f)
                reflectiveQuadTo(800f, 740f)
                reflectiveQuadToRelative(-17.5f, -42.5f)
                reflectiveQuadTo(740f, 680f)
                reflectiveQuadToRelative(-42.5f, 17.5f)
                reflectiveQuadTo(680f, 740f)
                reflectiveQuadToRelative(17.5f, 42.5f)
                reflectiveQuadTo(740f, 800f)
                moveTo(480f, 280f)
                quadToRelative(25f, 0f, 42.5f, -17.5f)
                reflectiveQuadTo(540f, 220f)
                reflectiveQuadToRelative(-17.5f, -42.5f)
                reflectiveQuadTo(480f, 160f)
                reflectiveQuadToRelative(-42.5f, 17.5f)
                reflectiveQuadTo(420f, 220f)
                reflectiveQuadToRelative(17.5f, 42.5f)
                reflectiveQuadTo(480f, 280f)
                moveTo(220f, 800f)
                quadToRelative(25f, 0f, 42.5f, -17.5f)
                reflectiveQuadTo(280f, 740f)
                reflectiveQuadToRelative(-17.5f, -42.5f)
                reflectiveQuadTo(220f, 680f)
                reflectiveQuadToRelative(-42.5f, 17.5f)
                reflectiveQuadTo(160f, 740f)
                reflectiveQuadToRelative(17.5f, 42.5f)
                reflectiveQuadTo(220f, 800f)
            }
        }.build()
        return _NetworkNode!!
    }

private var _NetworkNode: ImageVector? = null
