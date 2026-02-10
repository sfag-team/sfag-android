package com.sfag.shared.presentation.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun icon(name: String, block: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color.Black), pathBuilder = block)
    }.build()

val ChevronLeft by lazy {
    icon("Chevron_left") {
        moveTo(560f, 720f)
        lineTo(320f, 480f)
        lineToRelative(240f, -240f)
        lineToRelative(56f, 56f)
        lineToRelative(-184f, 184f)
        lineToRelative(184f, 184f)
        close()
    }
}

val ChevronRight by lazy {
    icon("Chevron_right") {
        moveTo(504f, 480f)
        lineTo(320f, 296f)
        lineToRelative(56f, -56f)
        lineToRelative(240f, 240f)
        lineToRelative(-240f, 240f)
        lineToRelative(-56f, -56f)
        close()
    }
}

val FileOpen by lazy {
    icon("File_open") {
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
}

val FileSave by lazy {
    icon("File_save") {
        moveTo(720f, 840f)
        lineToRelative(160f, -160f)
        lineToRelative(-56f, -56f)
        lineToRelative(-64f, 64f)
        verticalLineToRelative(-167f)
        horizontalLineToRelative(-80f)
        verticalLineToRelative(167f)
        lineToRelative(-64f, -64f)
        lineToRelative(-56f, 56f)
        close()
        moveTo(560f, 960f)
        verticalLineToRelative(-80f)
        horizontalLineToRelative(320f)
        verticalLineTo(960f)
        close()
        moveTo(240f, 800f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(160f, 720f)
        verticalLineToRelative(-560f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(240f, 80f)
        horizontalLineToRelative(280f)
        lineToRelative(240f, 240f)
        verticalLineToRelative(121f)
        horizontalLineToRelative(-80f)
        verticalLineToRelative(-81f)
        horizontalLineTo(480f)
        verticalLineToRelative(-200f)
        horizontalLineTo(240f)
        verticalLineToRelative(560f)
        horizontalLineToRelative(240f)
        verticalLineToRelative(80f)
        close()
        moveToRelative(0f, -80f)
        verticalLineToRelative(-560f)
        close()
    }
}

val KeyboardDoubleArrowRight by lazy {
    icon("Keyboard_double_arrow_right") {
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
}

val NetworkNode by lazy {
    icon("Network_node") {
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
}
