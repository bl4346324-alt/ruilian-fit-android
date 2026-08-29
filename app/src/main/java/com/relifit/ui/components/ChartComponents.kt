package com.relifit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relifit.util.TimeUtils

/**
 * 图表组件（纯 Compose Canvas 绘制，零第三方库，PRD 要求）
 */

/** 折线图（重量进步曲线）：渐变面积 + 圆点 + PR 新纪录标记 */
@Composable
fun LineChart(
    values: List<Float>,
    labels: List<String>,
    prIndex: Int = -1,          // 新纪录点下标
    modifier: Modifier = Modifier
) {
    if (values.size < 2) {
        Box(modifier = modifier.height(180.dp), contentAlignment = Alignment.Center) {
            Text("数据不足，完成一次训练后生成曲线", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        return
    }
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.surfaceContainerHigh
    val axisText = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val minV = values.min()
    val maxV = values.max()
    val range = (maxV - minV).coerceAtLeast(1f)

    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        val w = size.width
        val h = size.height
        val padX = 24.dp.toPx()
        val padTop = 28.dp.toPx()
        val padBottom = 30.dp.toPx()
        val chartH = h - padTop - padBottom
        val stepX = (w - padX * 2) / (values.size - 1)

        fun yOf(v: Float) = padTop + chartH - (v - minV) / range * chartH

        // 网格线（3 条）
        for (i in 0..3) {
            val y = padTop + chartH * i / 3f
            drawLine(grid, Offset(padX, y), Offset(w - padX, y), 1.dp.toPx())
        }
        // 渐变面积
        val area = Path()
        values.forEachIndexed { i, v ->
            val x = padX + stepX * i
            val y = yOf(v)
            if (i == 0) area.moveTo(x, y) else area.lineTo(x, y)
        }
        area.lineTo(padX + stepX * (values.size - 1), padTop + chartH)
        area.lineTo(padX, padTop + chartH)
        area.close()
        drawPath(
            area,
            Brush.verticalGradient(
                listOf(primary.copy(alpha = 0.28f), Color.Transparent),
                startY = padTop, endY = padTop + chartH
            )
        )
        // 折线
        val line = Path()
        values.forEachIndexed { i, v ->
            val x = padX + stepX * i
            val y = yOf(v)
            if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }
        drawPath(line, primary, style = Stroke(4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
        // 数据点 + 数值
        values.forEachIndexed { i, v ->
            val x = padX + stepX * i
            val y = yOf(v)
            val isPr = i == prIndex
            drawCircle(
                color = if (isPr) primary else surfaceColor,
                radius = if (isPr) 8.dp.toPx() else 5.dp.toPx(),
                center = Offset(x, y)
            )
            if (isPr) drawCircle(Color.White, 3.dp.toPx(), Offset(x, y))
        }
    }
    // 数值标注 + 底部标签
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        values.forEachIndexed { i, v ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (v % 1f == 0f) v.toInt().toString() else v.toString(),
                    fontSize = 11.sp,
                    fontWeight = if (i == prIndex) FontWeight.Bold else FontWeight.Medium,
                    color = if (i == prIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(labels.getOrElse(i) { "" }, fontSize = 11.sp, color = axisText)
            }
        }
    }
}

/**
 * 柱状图（训练频率 / 近 7 天热量）：每根柱带数值标注，可高亮某根（今天）
 */
@Composable
fun BarChart(
    values: List<Float>,
    labels: List<String>,
    highlightIndex: Int = -1,
    modifier: Modifier = Modifier
) {
    if (values.isEmpty()) return
    val maxV = values.max().coerceAtLeast(1f)
    val primary = MaterialTheme.colorScheme.primary
    val idleBar = MaterialTheme.colorScheme.surfaceContainerHigh

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { i, v ->
            val isHl = i == highlightIndex || (highlightIndex < 0 && v > 0)
            val isZero = v <= 0
            val barHeight = if (isZero) 10.dp else (36 + 90 * (v / maxV)).dp
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (v > 0) TimeUtils.thousands(v.toDouble()) else "",
                    fontSize = 10.sp,
                    fontWeight = if (isHl) FontWeight.Bold else FontWeight.Medium,
                    color = if (isHl) primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(barHeight)
                        .then(
                            if (isHl) Modifier.background(
                                Brush.verticalGradient(listOf(primary, Color(0xFF6E9CC4))),
                                RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                            ) else Modifier.background(
                                if (isZero) idleBar.copy(alpha = 0.5f) else idleBar,
                                RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                            )
                        )
                )
                Spacer(Modifier.height(6.dp))
                Text(labels.getOrElse(i) { "" }, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * 肌群分布横向进度条列表（Demo 肌群覆盖）
 */
@Composable
fun HBarList(
    items: List<Pair<String, Float>>,   // 名称 -> 占比 0f-1f
    valueText: List<String>,            // 右侧数值文案
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        items.forEachIndexed { i, (name, ratio) ->
            Column {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(
                        valueText.getOrElse(i) { "" },
                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(8.dp))
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(50))
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio.coerceIn(0f, 1f))
                            .height(12.dp)
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFF6E9CC4), MaterialTheme.colorScheme.primary)),
                                RoundedCornerShape(50)
                            )
                    )
                }
            }
        }
    }
}

/** 图表卡片容器（统计页统一卡片） */
@Composable
fun ChartCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .softCardShadow(28)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (badge != null) {
                Text(
                    text = badge,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        content()
    }
}
