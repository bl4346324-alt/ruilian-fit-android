package com.relifit.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 形状规范 —— 对齐 Demo：大量 28-32dp 大圆角卡片
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),   // 次级卡片
    extraLarge = RoundedCornerShape(32.dp) // 主卡片（Demo 卡片圆角）
)
