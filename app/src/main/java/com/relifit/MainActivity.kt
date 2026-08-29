package com.relifit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relifit.ui.navigation.AppNavGraph
import com.relifit.ui.theme.ReliFitTheme
import kotlinx.coroutines.launch

/**
 * 唯一 Activity：承载 Compose 导航图
 * 主题由 DataStore 设置驱动（system/light/dark），切换后全局生效
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = ReliFitApp.from(this)

        setContent {
            // 主题模式流 -> 当前是否深色
            val themeMode by app.settingsRepository.themeMode.collectAsStateWithLifecycle(initialValue = "system")
            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            val scope = rememberCoroutineScope()

            ReliFitTheme(darkTheme = darkTheme) {
                AppNavGraph(
                    darkTheme = darkTheme,
                    // 顶栏主题切换按钮：当前深色则切浅色，反之切深色
                    onToggleTheme = {
                        val target = if (darkTheme) "light" else "dark"
                        scope.launch { app.settingsRepository.setThemeMode(target) }
                    }
                )
            }
        }
    }
}
