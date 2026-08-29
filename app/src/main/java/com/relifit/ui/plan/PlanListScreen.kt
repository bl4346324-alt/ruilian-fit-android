package com.relifit.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.relifit.data.local.entity.WorkoutPlan
import com.relifit.ui.components.AppChip
import com.relifit.ui.components.softCardShadow
import com.relifit.ui.theme.LocalExtraColors
import kotlinx.coroutines.flow.collectLatest

/** 计划类型分类（力量 / 核心 / 有氧 / 恢复） */
val planTypes = listOf("力量", "核心", "有氧", "恢复")

/** 各类型对应的展示色 */
@Composable
private fun typeColor(type: String): Color = when (type) {
    "力量" -> MaterialTheme.colorScheme.primary
    "核心" -> LocalExtraColors.current.success
    "有氧" -> LocalExtraColors.current.amber
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

/**
 * 计划列表页：查看/新建/复制训练计划（按类型分类展示）
 */
@Composable
fun PlanListScreen(
    onBack: () -> Unit,
    onOpenPlan: (Long) -> Unit,
    viewModel: PlanListViewModel = viewModel(factory = PlanListViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showCreate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collectLatest { snackbar.showSnackbar(it) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 20.dp)
        ) {
            // 顶栏
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
                    "训练计划",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { showCreate = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("新建计划", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // 计划类型说明
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                planTypes.forEach { t ->
                    Box(
                        modifier = Modifier
                            .background(typeColor(t).copy(alpha = 0.14f), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(t, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = typeColor(t))
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.plans, key = { it.id }) { plan ->
                    PlanRow(
                        plan = plan,
                        onClick = { onOpenPlan(plan.id) },
                        onCopy = { if (plan.isTemplate) viewModel.copyTemplate(plan) }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    // ===== 新建计划弹窗 =====
    if (showCreate) {
        var name by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("力量") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("新建训练计划") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("计划名称（如：增肌周期一）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("计划类型", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        planTypes.forEach { t ->
                            AppChip(text = t, selected = type == t, onClick = { type = t })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        viewModel.createPlan(name.trim(), type)
                        showCreate = false
                    }
                }) { Text("创建") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("取消") } }
        )
    }
}

/** 计划卡片行：名称 + 类型徽章 + 周期信息 + （模板）复制按钮 */
@Composable
private fun PlanRow(plan: WorkoutPlan, onClick: () -> Unit, onCopy: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .softCardShadow(24)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(plan.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(8.dp))
                Text(
                    plan.type,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = typeColor(plan.type),
                    modifier = Modifier
                        .background(typeColor(plan.type).copy(alpha = 0.14f), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${if (plan.isTemplate) "内置模板" else "自定义"} · ${plan.cycleWeeks} 周 · 每周 ${plan.daysPerWeek} 练 · ${plan.targetDurationMin} 分钟",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (plan.isTemplate) {
            IconButton(onClick = onCopy) {
                Icon(Icons.Filled.ContentCopy, "复制模板", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}
