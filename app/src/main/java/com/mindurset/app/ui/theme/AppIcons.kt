package com.mindurset.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * One monoline icon set for the whole app: 24x24 grid, 1.75 dp stroke, round caps.
 * Draw them with [androidx.compose.material3.Icon] so the tint follows the theme.
 */
object AppIcons {

    private const val STROKE = 1.75f

    private fun icon(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            paths.forEach { d ->
                addPath(
                    pathData = PathParser().parsePathString(d).toNodes(),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = STROKE,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    val Home: ImageVector by lazy {
        icon("Home", "M4 10.5 L12 4 L20 10.5", "M6.5 10 V19.5 H17.5 V10")
    }
    val Therapy: ImageVector by lazy {
        icon(
            "Therapy",
            "M12 19.5 C12 19.5 5 15.6 5 10.6 A3.9 3.9 0 0 1 12 8.4 A3.9 3.9 0 0 1 19 10.6 C19 15.6 12 19.5 12 19.5 z",
        )
    }
    val Companion: ImageVector by lazy {
        icon("Companion", "M4.5 6 h15 v10.5 h-8 l-4.5 3.5 v-3.5 h-2.5 z")
    }
    val Profile: ImageVector by lazy {
        icon(
            "Profile",
            "M12 4.6 a3.5 3.5 0 1 0 0 7 a3.5 3.5 0 1 0 0 -7",
            "M5 20 c0-3.6 3.1-5.6 7-5.6 s7 2 7 5.6",
        )
    }
    val Menu: ImageVector by lazy {
        icon("Menu", "M4 7 h16", "M4 12 h16", "M4 17 h16")
    }
    val Settings: ImageVector by lazy {
        icon(
            "Settings",
            "M4 8 h8", "M17 8 h3", "M4 16 h3", "M12 16 h8",
            "M14.5 6 a2 2 0 1 0 0 4 a2 2 0 1 0 0 -4",
            "M9.5 14 a2 2 0 1 0 0 4 a2 2 0 1 0 0 -4",
        )
    }
    val Calendar: ImageVector by lazy {
        icon("Calendar", "M4.5 6.5 h15 v13 h-15 z", "M4.5 10.5 h15", "M8.5 4.5 v4", "M15.5 4.5 v4")
    }
    val Edit: ImageVector by lazy {
        icon("Edit", "M5.5 18.5 h3 L19 8 l-3-3 L5.5 15.5 z", "M14.5 6.5 l3 3")
    }
    val Trash: ImageVector by lazy {
        icon("Trash", "M5.5 8 h13", "M10 8 V5.5 h4 V8", "M7.5 8 l1 11.5 h7 l1 -11.5")
    }
    val Add: ImageVector by lazy {
        icon("Add", "M12 5.5 v13", "M5.5 12 h13")
    }
    val Check: ImageVector by lazy {
        icon("Check", "M5 12.5 l4.5 4.5 L19 7")
    }
    val Close: ImageVector by lazy {
        icon("Close", "M6.5 6.5 l11 11", "M17.5 6.5 l-11 11")
    }
    val ChevronRight: ImageVector by lazy {
        icon("ChevronRight", "M9.5 6 l6 6 -6 6")
    }
    val ArrowBack: ImageVector by lazy {
        icon("ArrowBack", "M14.5 6 l-6 6 6 6")
    }
    val Bell: ImageVector by lazy {
        icon(
            "Bell",
            "M12 4.2 c2.6 0 4.7 2.1 4.7 4.7 v3.6 l1.6 2.6 H5.7 l1.6-2.6 V8.9 c0-2.6 2.1-4.7 4.7-4.7 z",
            "M9.8 17.6 a2.2 2.2 0 0 0 4.4 0",
        )
    }
    val Sparkles: ImageVector by lazy {
        icon(
            "Sparkles",
            "M11 4 l1.7 4.3 L17 10 l-4.3 1.7 L11 16 l-1.7-4.3 L5 10 l4.3-1.7 z",
            "M18 15 l0.9 2.1 L21 18 l-2.1 0.9 L18 21 l-0.9-2.1 L15 18 l2.1-0.9 z",
        )
    }
    val Book: ImageVector by lazy {
        icon(
            "Book",
            "M6 4.5 h11 a1.5 1.5 0 0 1 1.5 1.5 v13 H7.5 A1.5 1.5 0 0 1 6 17.5 z",
            "M6 17.5 A1.5 1.5 0 0 1 7.5 16 H18.5",
        )
    }
    val Clock: ImageVector by lazy {
        icon("Clock", "M12 4.5 a7.5 7.5 0 1 0 0 15 a7.5 7.5 0 1 0 0 -15", "M12 8 v4.4 l3 1.8")
    }
    val Globe: ImageVector by lazy {
        icon(
            "Globe",
            "M12 4.5 a7.5 7.5 0 1 0 0 15 a7.5 7.5 0 1 0 0 -15",
            "M4.6 12 h14.8",
            "M12 4.5 c2.4 2.6 2.4 12.4 0 15 c-2.4-2.6-2.4-12.4 0-15",
        )
    }
    val Shield: ImageVector by lazy {
        icon(
            "Shield",
            "M12 4.5 l6.5 2.4 v4.6 c0 3.7-2.8 6.1-6.5 7.5 c-3.7-1.4-6.5-3.8-6.5-7.5 V6.9 z",
        )
    }
    val Refresh: ImageVector by lazy {
        icon("Refresh", "M19.5 12 a7.5 7.5 0 1 1 -2.6-5.7", "M19.5 4.5 v3.6 h-3.6")
    }
    val Info: ImageVector by lazy {
        icon("Info", "M12 4.5 a7.5 7.5 0 1 0 0 15 a7.5 7.5 0 1 0 0 -15", "M12 11 v5.5", "M12 8.1 v0.01")
    }
    val Chart: ImageVector by lazy {
        icon("Chart", "M4.5 19 h15", "M4.5 15.5 l4.5-4.5 3 3 6.5-7")
    }
    val FaceHappy: ImageVector by lazy {
        icon(
            "FaceHappy",
            "M12 3.5 a8.5 8.5 0 1 0 0 17 a8.5 8.5 0 1 0 0 -17",
            "M9 9.3 a0.9 0.9 0 1 0 0 1.8 a0.9 0.9 0 1 0 0 -1.8",
            "M15 9.3 a0.9 0.9 0 1 0 0 1.8 a0.9 0.9 0 1 0 0 -1.8",
            "M8 14 q4 4 8 0",
        )
    }
    val FaceCalm: ImageVector by lazy {
        icon(
            "FaceCalm",
            "M12 3.5 a8.5 8.5 0 1 0 0 17 a8.5 8.5 0 1 0 0 -17",
            "M9 9.3 a0.9 0.9 0 1 0 0 1.8 a0.9 0.9 0 1 0 0 -1.8",
            "M15 9.3 a0.9 0.9 0 1 0 0 1.8 a0.9 0.9 0 1 0 0 -1.8",
            "M8.5 14.6 h7",
        )
    }
    val FaceLow: ImageVector by lazy {
        icon(
            "FaceLow",
            "M12 3.5 a8.5 8.5 0 1 0 0 17 a8.5 8.5 0 1 0 0 -17",
            "M9 9.3 a0.9 0.9 0 1 0 0 1.8 a0.9 0.9 0 1 0 0 -1.8",
            "M15 9.3 a0.9 0.9 0 1 0 0 1.8 a0.9 0.9 0 1 0 0 -1.8",
            "M8 15.6 q4 -3 8 0",
        )
    }
    val FaceAnxious: ImageVector by lazy {
        icon(
            "FaceAnxious",
            "M12 3.5 a8.5 8.5 0 1 0 0 17 a8.5 8.5 0 1 0 0 -17",
            "M9 9.3 a0.9 0.9 0 1 0 0 1.8 a0.9 0.9 0 1 0 0 -1.8",
            "M15 9.3 a0.9 0.9 0 1 0 0 1.8 a0.9 0.9 0 1 0 0 -1.8",
            "M8 14.6 q2 -1.8 4 0 t4 0",
        )
    }
    val FaceStressed: ImageVector by lazy {
        icon(
            "FaceStressed",
            "M12 3.5 a8.5 8.5 0 1 0 0 17 a8.5 8.5 0 1 0 0 -17",
            "M9 9.3 a0.9 0.9 0 1 0 0 1.8 a0.9 0.9 0 1 0 0 -1.8",
            "M15 9.3 a0.9 0.9 0 1 0 0 1.8 a0.9 0.9 0 1 0 0 -1.8",
            "M8 15.2 l2 -2 2 2 2 -2",
        )
    }
    val Eye: ImageVector by lazy {
        icon(
            "Eye",
            "M2.8 12 C5 8.4 8.3 6.4 12 6.4 C15.7 6.4 19 8.4 21.2 12 C19 15.6 15.7 17.6 12 17.6 C8.3 17.6 5 15.6 2.8 12 z",
            "M12 9.2 a2.8 2.8 0 1 0 0 5.6 a2.8 2.8 0 1 0 0 -5.6",
        )
    }
    val EyeOff: ImageVector by lazy {
        icon(
            "EyeOff",
            "M4 4.5 L20 19.5",
            "M9.4 6.9 C10.2 6.6 11.1 6.4 12 6.4 C15.7 6.4 19 8.4 21.2 12 C20.4 13.4 19.4 14.6 18.2 15.5",
            "M15.4 16.9 C14.3 17.4 13.2 17.6 12 17.6 C8.3 17.6 5 15.6 2.8 12 C3.7 10.5 4.9 9.2 6.3 8.3",
        )
    }
    val Logout: ImageVector by lazy {
        icon("Logout", "M14.5 6.5 H7 v11 h7.5", "M12.5 12 h8", "M17.5 8.5 l3.5 3.5 -3.5 3.5")
    }
}
