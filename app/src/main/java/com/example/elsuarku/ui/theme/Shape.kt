package com.example.elsuarku.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * ElSuarKu Shape System — consistent corner radii across the app.
 *
 * Scale:
 *   xs  = 4dp   — inline chips, tiny badges
 *   sm  = 8dp   — small cards, input fields, status badges
 *   md  = 12dp  — standard cards, dialogs, bottom sheets
 *   lg  = 16dp  — large cards, dashboard widgets
 *   xl  = 20dp  — hero cards, featured sections
 *   2xl = 28dp  — modal sheets top corners
 *   full = 50%  — pills, circular elements
 */
val ShapeXSmall = RoundedCornerShape(4.dp)
val ShapeSmall = RoundedCornerShape(8.dp)
val ShapeMedium = RoundedCornerShape(12.dp)
val ShapeLarge = RoundedCornerShape(16.dp)
val ShapeXLarge = RoundedCornerShape(20.dp)
val Shape2XLarge = RoundedCornerShape(28.dp)
val ShapePill = RoundedCornerShape(50)

val AppShapes = Shapes(
    extraSmall = ShapeXSmall,
    small = ShapeSmall,
    medium = ShapeMedium,
    large = ShapeLarge,
    extraLarge = ShapeXLarge
)
