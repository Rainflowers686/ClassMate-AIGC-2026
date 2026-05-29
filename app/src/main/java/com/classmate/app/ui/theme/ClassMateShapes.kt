package com.classmate.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class ClassMateShapes(
    val small: RoundedCornerShape = RoundedCornerShape(8),
    val medium: RoundedCornerShape = RoundedCornerShape(12),
    val large: RoundedCornerShape = RoundedCornerShape(16),
    val hero: RoundedCornerShape = RoundedCornerShape(20),
    val pill: RoundedCornerShape = RoundedCornerShape(percent = 50)
)

val LocalClassMateShapes = staticCompositionLocalOf { ClassMateShapes() }
