package com.relifit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 通用卡片：32dp 大圆角 + 柔和阴影（Demo 卡片式布局核心组件）
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 32,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    val base = Modifier
        .shadow(10.dp, shape)
        .background(MaterialTheme.colorScheme.surface, shape)
    Box(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base
    ) {
        content()
    }
}

/**
 * 区块标题：左侧标题 + 右侧"全部 >"式链接（Demo sec-title）
 */
@Composable
fun SectionTitle(
    title: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (actionText != null && onAction != null) {
            Row(
                modifier = Modifier.clickable(onClick = onAction),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 数据小卡片（Demo 统计卡：图标 + 环比 + 大数字 + 标签）
 */
@Composable
fun StatCard(
    icon: ImageVector,
    iconTint: Color,
    delta: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(iconTint.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = delta,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 筛选/肌群标签（Demo chip：选中为主色容器）
 */
@Composable
fun AppChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(50))
            .background(bg, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 9.dp)
    ) {
        Text(text = text, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * 环形进度（Demo 计划进度环）
 * @param progress 0f-1f
 */
@Composable
fun RingProgress(
    progress: Float,
    centerTop: String,
    centerBottom: String,
    modifier: Modifier = Modifier,
    size: Int = 92
) {
    Box(modifier = modifier.size(size.dp), contentAlignment = Alignment.Center) {
        // 提前取出颜色（Canvas 绘制 lambda 中不能直接引用 Composable 上下文）
        val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
        val ringColor = MaterialTheme.colorScheme.primary
        androidx.compose.foundation.Canvas(modifier = Modifier.size(size.dp)) {
            val stroke = 10.dp.toPx()
            val arcSize = size.dp.toPx() - stroke
            drawArc(
                color = trackColor,
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f, sweepAngle = 360f * progress.coerceIn(0f, 1f), useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerTop, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Text(centerBottom, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * 水平进度条（Demo 进度条 / 肌群分布条）
 */
@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Int = 10,
    gradient: Boolean = true
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .height(height.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(height.dp)
                .then(
                    if (gradient) Modifier.background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF6E9CC4), MaterialTheme.colorScheme.primary)
                        ),
                        shape
                    ) else Modifier.background(MaterialTheme.colorScheme.primary, shape)
                )
        )
    }
}

/** 间隔占位 */
@Composable
fun Spacer16() = Spacer(Modifier.height(16.dp))
@Composable
fun Spacer8() = Spacer(Modifier.height(8.dp))
@Composable
fun Spacer24() = Spacer(Modifier.height(24.dp))

/**
 * 卡片柔和阴影（整体一致性优化用）
 * 浅色模式下给卡片柔和立体感；深色模式下阴影自然弱化，符合 M3 规范
 */
fun Modifier.softCardShadow(corner: Int = 24): Modifier =
    this.then(Modifier.shadow(8.dp, RoundedCornerShape(corner.dp)))
