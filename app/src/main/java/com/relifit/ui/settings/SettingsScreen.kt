package com.relifit.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.relifit.ui.components.softCardShadow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 系统设置页（PRD 系统设置模块）
 * 主题（跟随系统/浅色/深色）、单位 kg/lb、默认休息秒数、关于与隐私、清除数据
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showClearConfirm by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = viewModel.exportBackup()
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(json.toByteArray(Charsets.UTF_8))
                    }
                    snackbar.showSnackbar("数据备份已成功导出")
                } catch (e: Exception) {
                    snackbar.showSnackbar("导出失败: ${e.message}")
                }
            }
        }
    }

    val exportWorkoutCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val csv = viewModel.exportWorkoutLogsCsv()
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(csv.toByteArray(Charsets.UTF_8))
                    }
                    snackbar.showSnackbar("训练记录 CSV 已成功导出")
                } catch (e: Exception) {
                    snackbar.showSnackbar("导出 CSV 失败: ${e.message}")
                }
            }
        }
    }

    val exportBodyCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val csv = viewModel.exportBodyMetricsCsv()
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(csv.toByteArray(Charsets.UTF_8))
                    }
                    snackbar.showSnackbar("身体数据 CSV 已成功导出")
                } catch (e: Exception) {
                    snackbar.showSnackbar("导出 CSV 失败: ${e.message}")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = context.contentResolver.openInputStream(uri)?.use {
                        it.bufferedReader().readText()
                    }
                    if (!json.isNullOrBlank()) {
                        pendingImportJson = json
                        showImportConfirm = true
                    } else {
                        snackbar.showSnackbar("无法读取备份文件内容")
                    }
                } catch (e: Exception) {
                    snackbar.showSnackbar("读取备份文件失败: ${e.message}")
                }
            }
        }
    }

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
                Text("设置", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }

            // ===== 主题 =====
            SettingsSection("外观") {
                listOf("system" to "跟随系统", "light" to "浅色模式", "dark" to "深色模式").forEach { (mode, label) ->
                    SettingsRow(label = label, selected = state.themeMode == mode) { viewModel.setThemeMode(mode) }
                }
            }

            // ===== 单位 =====
            SettingsSection("单位") {
                SettingsRow("公斤（kg）", selected = state.unit == "kg", indent = true) { viewModel.setUnit("kg") }
                SettingsRow("磅（lb）", selected = state.unit == "lb", indent = true) { viewModel.setUnit("lb") }
            }

            // ===== 组间休息默认值 =====
            SettingsSection("计时器") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("默认组间休息", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.setDefaultRestSec(state.defaultRestSec - 15) }) { Text("-15s") }
                    Text("${state.defaultRestSec}s", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(64.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    TextButton(onClick = { viewModel.setDefaultRestSec(state.defaultRestSec + 15) }) { Text("+15s") }
                }
            }

            // ===== DeepSeek AI 营养引擎 =====
            SettingsSection("DeepSeek AI 营养助手") {
                Text(
                    "配置您的 DeepSeek API Key 后，在饮食记录中可一键让 AI 智能估算任意食物的热量与三大营养素，并支持 AI 一键生成严格控油控盐的高蛋白健身菜谱。",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showApiKeyDialog = true }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("DeepSeek AI 引擎", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        val statusText = when {
                            state.isUsingCustomKey -> "自定义 Key（已启用）"
                            state.isUsingBuiltInKey -> "系统内置服务 · 已就绪 (免配置)"
                            else -> "未配置（点击设置）"
                        }
                        val statusColor = when {
                            state.isUsingCustomKey -> MaterialTheme.colorScheme.primary
                            state.isUsingBuiltInKey -> Color(0xFF43A047)
                            else -> MaterialTheme.colorScheme.error
                        }
                        Text(
                            statusText,
                            fontSize = 12.sp,
                            color = statusColor
                        )
                    }
                    TextButton(onClick = { showApiKeyDialog = true }) {
                        Text(if (state.isUsingCustomKey) "修改 Key" else "自定义 Key")
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("当前模型", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${state.deepseekModel} (DeepSeek-V3)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (state.isAiAvailable) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { viewModel.testDeepSeekKey() },
                            enabled = !state.isTestingKey
                        ) {
                            if (state.isTestingKey) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(6.dp))
                                Text("测试连接中...")
                            } else {
                                Text("测试 AI 服务连接")
                            }
                        }
                    }
                }
            }

            // ===== 数据备份与恢复 =====
            SettingsSection("数据备份与恢复") {
                Text(
                    "支持将所有训练记录、计划、身体与饮食数据导出为 JSON 备份文件，换机或重装时可一键导入恢复。",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                // 导出备份（保存文件）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val timeStr = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())
                            exportLauncher.launch("relifit_backup_$timeStr.json")
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.FileDownload, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("导出数据全量备份 (.json)", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                }
                // 导出训练记录 CSV 表格（Excel / WPS 可直接打开）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val timeStr = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())
                            exportWorkoutCsvLauncher.launch("relifit_workouts_$timeStr.csv")
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.FileDownload, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("导出训练记录 (.csv)", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("明细日志与每组数据，可用 Excel / WPS / Notion 打开", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                // 导出身体数据 CSV 表格
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val timeStr = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())
                            exportBodyCsvLauncher.launch("relifit_body_$timeStr.csv")
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.FileDownload, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("导出身体数据 (.csv)", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("体重、身高与每日运动量历史记录", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                // 分享备份文本
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch {
                                val json = viewModel.exportBackup()
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "锐炼Fit 数据备份")
                                    putExtra(Intent.EXTRA_TEXT, json)
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "分享或发送备份数据")
                                context.startActivity(shareIntent)
                            }
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Share, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("分享备份至其他应用", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                }
                // 导入备份文件
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.FileUpload, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("导入备份恢复数据", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                }
            }

            // ===== 关于 =====
            SettingsSection("关于") {
                SettingsRow("版本", value = com.relifit.BuildConfig.VERSION_NAME)
                Spacer(Modifier.height(12.dp))
                Text(
                    "隐私说明：锐炼Fit 是纯本地工具应用，所有训练、身体与饮食数据仅保存在您的设备上（Room 数据库），无需注册登录，不联网、不上传任何数据，无广告、无社交社区。",
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                // 清除数据
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showClearConfirm = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("清除全部本地数据", color = MaterialTheme.colorScheme.error, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清除全部数据？") },
            text = { Text("将删除所有训练记录、身体数据与饮食记录，并恢复默认动作库与模板。此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData()
                    showClearConfirm = false
                }) { Text("清除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("取消") }
            }
        )
    }

    if (showImportConfirm && pendingImportJson != null) {
        AlertDialog(
            onDismissRequest = {
                showImportConfirm = false
                pendingImportJson = null
            },
            title = { Text("恢复数据备份？") },
            text = { Text("导入备份将覆盖现有的本地数据（包括训练记录、自定义计划、身体与饮食记录）。确定继续吗？") },
            confirmButton = {
                TextButton(onClick = {
                    val json = pendingImportJson
                    showImportConfirm = false
                    pendingImportJson = null
                    if (json != null) {
                        viewModel.importBackup(json)
                    }
                }) {
                    Text("恢复", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    pendingImportJson = null
                }) {
                    Text("取消")
                }
            }
        )
    }

    if (showApiKeyDialog) {
        var tempKey by remember { mutableStateOf("") }
        var showKey by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Text(
                    if (state.isUsingCustomKey) "修改自定义 API Key" else "配置自定义 API Key",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val desc = if (state.isUsingBuiltInKey) {
                        "本应用已内置默认系统 AI 引擎，食物营养估算与智能菜谱开箱即用。\n若您希望使用自己的 DeepSeek 账号额度，可在下方输入您的专属 Key 进行覆盖；无需配置可直接点击取消。"
                    } else if (state.isUsingCustomKey) {
                        "当前正在使用您自定义的 API Key。输入新 Key 可更新；若想恢复使用系统内置的默认服务，可点击下方「恢复内置服务」。"
                    } else {
                        "前往 platform.deepseek.com 获取您的 API Key。Key 仅保存在本地设备，用于请求食物营养分析与菜谱生成。"
                    }
                    Text(
                        desc,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = { tempKey = it.trim() },
                        label = { Text("自定义 API Key (sk-...)") },
                        placeholder = { Text("输入新的 sk-... 覆盖默认配置") },
                        singleLine = true,
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    imageVector = if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showKey) "隐藏" else "显示",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.isUsingCustomKey) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    viewModel.clearCustomDeepSeekApiKey()
                                    showApiKeyDialog = false
                                }
                            ) {
                                Text("恢复使用系统内置服务", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempKey.isNotBlank()) {
                            viewModel.setDeepSeekApiKey(tempKey)
                        }
                        showApiKeyDialog = false
                    },
                    enabled = tempKey.isNotBlank()
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .softCardShadow(24)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun SettingsRow(
    label: String,
    selected: Boolean = false,
    value: String? = null,
    indent: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = if (indent) 16.dp else 0.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 15.sp,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
    }
}
