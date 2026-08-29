package com.relifit.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.relifit.data.local.entity.Exercise
import com.relifit.ui.components.AppChip
import com.relifit.ui.components.ScreenTopBar
import com.relifit.ui.components.softCardShadow
import kotlinx.coroutines.flow.collectLatest

/** 肌群标签顺序（Demo：胸默认选中，全部放最后；含新增的有氧/恢复类） */
private val groups = listOf("胸", "背", "肩", "腿", "手臂", "核心", "有氧", "恢复", "全部")

/**
 * 动作库页（Demo 布局）：搜索框 + 横向肌群标签 + 动作卡片列表
 */
@Composable
fun LibraryScreen(
    onOpenExercise: (Long) -> Unit,
    onToggleTheme: () -> Unit,
    darkTheme: Boolean,
    viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    // 提示消息（离线包状态 / 下载结果）
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
            ScreenTopBar(
                title = "动作库",
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme,
                extraActions = {
                    // 离线包入口（Demo 云朵图标）
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
                            .clickable { viewModel.showOfflineInfo() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.CloudDone,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )

            // 搜索框（Demo search：圆角胶囊）
            TextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text("搜索动作名称、肌群、器械", fontSize = 15.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))

            // 肌群标签横向滚动
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groups.forEach { g ->
                    AppChip(
                        text = g,
                        selected = state.group == g,
                        onClick = { viewModel.selectGroup(g) }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // 动作列表
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.exercises, key = { it.id }) { ex ->
                    ExerciseRow(
                        ex = ex,
                        onClick = { onOpenExercise(ex.id) },
                        onToggleOffline = { viewModel.toggleOffline(ex.id) }
                    )
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

/** 动作卡片行（Demo ex-card：缩略图 + 名称 + 肌群·器械 + 难度 chip + 离线云图标 + 箭头） */
@Composable
private fun ExerciseRow(
    ex: Exercise,
    onClick: () -> Unit,
    onToggleOffline: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .softCardShadow(24)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 缩略占位图（演示区：哑铃图标）
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.FitnessCenter,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(ex.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${ex.muscleGroup} · ${ex.equipment}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    ex.difficulty,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        // 离线状态切换（Demo 云朵图标，点击下载/取消）
        Icon(
            imageVector = if (ex.offlineAvailable) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
            contentDescription = "离线下载",
            tint = if (ex.offlineAvailable) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(26.dp)
                .clickable(onClick = onToggleOffline)
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Filled.ChevronRight,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
    }
}
