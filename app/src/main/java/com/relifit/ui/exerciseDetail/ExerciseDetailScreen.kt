package com.relifit.ui.exerciseDetail

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.relifit.ui.components.AppChip
import com.relifit.ui.components.softCardShadow
import kotlinx.coroutines.flow.collectLatest

/**
 * 动作详情页（Demo 布局）：发力要点 + 易错提醒 + 呼吸节奏 + 离线下载 + 加入训练
 */
@Composable
fun ExerciseDetailScreen(
    exerciseId: Long,
    onBack: () -> Unit,
    onStartWorkout: (Long) -> Unit,
    viewModel: ExerciseDetailViewModel = viewModel(factory = ExerciseDetailViewModel.Factory)
) {
    val ex by viewModel.exercise.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // 顶栏：返回 + 标题 + 收藏
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
                    text = ex?.name ?: "动作详情",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { /* 收藏切换（P1） */ }) {
                    Icon(
                        if (ex?.isFavorite == true) Icons.Filled.Star else Icons.Filled.StarBorder,
                        "收藏",
                        tint = if (ex?.isFavorite == true) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 元信息 chips（肌群 / 器械 / 难度）
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppChip(ex?.muscleGroup ?: "", selected = true, onClick = {})
                AppChip(ex?.equipment ?: "", selected = true, onClick = {})
                AppChip(ex?.difficulty ?: "", selected = true, onClick = {})
            }

            Spacer(Modifier.height(18.dp))

            // 发力要点
            InfoCard(title = "发力要点", color = MaterialTheme.colorScheme.primary) {
                ex?.keyPoints?.split("\n")?.forEachIndexed { i, p ->
                    PointRow(num = (i + 1).toString(), text = p, warn = false)
                }
            }

            // 易错提醒
            InfoCard(title = "易错提醒", color = Color(0xFFE57373)) {
                ex?.mistakes?.split("\n")?.forEach { p ->
                    PointRow(num = "", text = p, warn = true)
                }
            }

            // 呼吸与节奏
            InfoCard(title = "呼吸与节奏", color = MaterialTheme.colorScheme.primary) {
                Text(
                    ex?.breathTip ?: "",
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))

            // 离线下载（Demo 底部主按钮）
            Button(
                onClick = { viewModel.toggleOffline() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Icon(
                    if (ex?.offlineAvailable == true) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                    null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (ex?.offlineAvailable == true) "已离线下载，无网可用" else "下载离线教学",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            // 加入训练（PRD：动作详情可加入训练列表）
            Button(
                onClick = { onStartWorkout(exerciseId) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text("用此动作开始训练", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 信息卡片（发力要点 / 易错提醒 / 呼吸） */
@Composable
private fun InfoCard(
    title: String,
    color: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .softCardShadow(24)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(8.dp))
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

/** 要点行：序号圆块（或警告图标）+ 文案 */
@Composable
private fun PointRow(num: String, text: String, warn: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (warn) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(Color(0xFFE57373).copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Warning, null, tint = Color(0xFFE57373), modifier = Modifier.size(16.dp))
            }
        } else {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(num, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 15.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
