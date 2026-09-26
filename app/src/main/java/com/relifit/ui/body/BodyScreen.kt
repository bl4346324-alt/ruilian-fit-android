package com.relifit.ui.body

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.relifit.ui.components.ChartCard
import com.relifit.ui.components.LineChart
import com.relifit.ui.components.softCardShadow
import com.relifit.util.TimeUtils
import kotlinx.coroutines.launch

/**
 * 身体数据页：体重 / 身高 / 每日运动数量 + 体重趋势图 + 记录按钮
 */
@Composable
fun BodyScreen(
    onBack: () -> Unit,
    viewModel: BodyViewModel = viewModel(factory = BodyViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showInput by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // 顶栏：返回 + 标题（记录入口统一在下方"记录新数据"按钮）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    "身体数据",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // ===== 最新三项指标 =====
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    icon = Icons.Filled.Scale,
                    value = state.latest?.weightKg?.let { String.format("%.1f", it) } ?: "--",
                    unit = "kg",
                    label = "体重",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    icon = Icons.Filled.SquareFoot,
                    value = state.latest?.heightCm?.let { String.format("%.1f", it) } ?: "--",
                    unit = "cm",
                    label = "身高",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    icon = Icons.Filled.Timer,
                    value = state.latest?.dailyActivity?.let { formatNum(it) } ?: "--",
                    unit = "",
                    label = "每日运动数量",
                    modifier = Modifier.weight(1f)
                )
            }

            // ===== BMI 指数卡片（基于身高体重自动核算） =====
            if (state.bmi != null) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .softCardShadow(20)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "BMI 指数",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(8.dp))
                            val badgeColor = when (state.bmiCategory) {
                                "正常" -> MaterialTheme.colorScheme.primary
                                "偏瘦" -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.error
                            }
                            Text(
                                state.bmiCategory,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                modifier = Modifier
                                    .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(50))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "中国成人健康标准范围：18.5 ~ 23.9",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = String.format("%.1f", state.bmi),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // ===== 体重趋势 =====
            ChartCard(
                title = "体重趋势",
                subtitle = if (state.weightValues.isNotEmpty()) "近 ${state.weightValues.size} 次记录" else "记录新数据后生成趋势"
            ) {
                LineChart(values = state.weightValues, labels = state.weightLabels)
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { showInput = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.padding(start = 6.dp))
                Text("记录新数据", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(24.dp))

            // ===== 历史记录列表 =====
            Text(
                "历史记录",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))

            if (state.metrics.isEmpty()) {
                Text(
                    "暂无历史记录",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .softCardShadow(20)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    state.metrics.forEachIndexed { idx, m ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    TimeUtils.formatDate(m.date),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(2.dp))
                                val detailParts = mutableListOf<String>()
                                m.weightKg?.let { detailParts.add("体重 ${String.format("%.1f", it)} kg") }
                                m.heightCm?.let { detailParts.add("身高 ${String.format("%.1f", it)} cm") }
                                m.dailyActivity?.let { detailParts.add("运动 ${formatNum(it)}") }
                                Text(
                                    if (detailParts.isEmpty()) "无具体数值" else detailParts.joinToString(" · "),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { recordToDelete = m.id },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "删除记录",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (idx < state.metrics.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ===== 录入弹窗：体重 / 身高 / 每日运动数量 =====
    if (showInput) {
        var w by remember { mutableStateOf("") }
        var h by remember { mutableStateOf("") }
        var act by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showInput = false },
            title = { Text("记录身体数据") },
            text = {
                Column {
                    NumField("体重（kg）", w) { w = it }
                    NumField("身高（cm）", h) { h = it }
                    NumField("每日运动数量", act) { act = it }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addMetric(w.toDoubleOrNull(), h.toDoubleOrNull(), act.toDoubleOrNull())
                    showInput = false
                    scope.launch { snackbar.showSnackbar("已记录身体数据") }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showInput = false }) { Text("取消") } }
        )
    }

    recordToDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("删除该条记录？") },
            text = { Text("删除后将无法恢复该时间点的身体数据。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(id)
                    recordToDelete = null
                    scope.launch { snackbar.showSnackbar("已删除身体数据记录") }
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun NumField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() || c == '.' }) },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )
}

@Composable
private fun MetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .softCardShadow(24)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .padding(vertical = 16.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (unit.isNotBlank()) {
                Text(" $unit", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatNum(v: Double): String =
    if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.1f", v)
