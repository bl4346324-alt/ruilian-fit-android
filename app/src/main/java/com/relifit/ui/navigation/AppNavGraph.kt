package com.relifit.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.relifit.ui.body.BodyScreen
import com.relifit.ui.diet.DietScreen
import com.relifit.ui.exerciseDetail.ExerciseDetailScreen
import com.relifit.ui.home.HomeScreen
import com.relifit.ui.library.LibraryScreen
import com.relifit.ui.logs.LogsScreen
import com.relifit.ui.plan.PlanDetailScreen
import com.relifit.ui.plan.PlanListScreen
import com.relifit.ui.settings.SettingsScreen
import com.relifit.ui.stats.StatsScreen
import com.relifit.ui.workout.WorkoutScreen

/** 底部导航 5 个 Tab 定义（与 Demo 顺序一致） */
private data class NavItem(val route: String, val label: String, val icon: ImageVector)

private val navItems = listOf(
    NavItem(Routes.HOME, "首页", Icons.Filled.Home),
    NavItem(Routes.LIBRARY, "动作库", Icons.Filled.FitnessCenter),
    NavItem(Routes.LOGS, "训练记录", Icons.Filled.ReceiptLong),
    NavItem(Routes.DIET, "饮食记录", Icons.Filled.Restaurant),
    NavItem(Routes.STATS, "数据统计", Icons.Filled.BarChart)
)

/**
 * 全局导航图：Scaffold + 底部导航 + NavHost
 * 子页面（训练进行中/动作详情/计划详情/身体数据/设置）隐藏底部导航，与 Demo 一致
 */
@Composable
fun AppNavGraph(darkTheme: Boolean, onToggleTheme: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in Routes.topLevel

    /**
     * 统一的顶部 Tab 导航（修复：页内"查看统计"若用普通 navigate 跳 Tab，
     * 会与底部导航的 saveState/restoreState 模式混用，导致返回该 Tab 时无响应）。
     * 底部导航与页内跳转都走这里，保证每个 Tab 只保留一份、状态一致。
     */
    val navigateToTab: (String) -> Unit = { route ->
        if (route != currentRoute) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    val currentDestination = backStackEntry?.destination
                    navItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateToTab(item.route) },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenPlan = { navController.navigate(Routes.plan(it)) },
                    onStartWorkout = { planId, dayId -> navController.navigate(Routes.workout(planId, dayId, null)) },
                    onOpenBody = { navController.navigate(Routes.BODY) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onToggleTheme = onToggleTheme,
                    darkTheme = darkTheme
                )
            }
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    onOpenExercise = { navController.navigate(Routes.exercise(it)) },
                    onToggleTheme = onToggleTheme,
                    darkTheme = darkTheme
                )
            }
            composable(Routes.LOGS) {
                LogsScreen(
                    onToggleTheme = onToggleTheme,
                    darkTheme = darkTheme,
                    onOpenPlans = { navController.navigate(Routes.PLANS) }
                )
            }
            composable(Routes.DIET) {
                DietScreen(
                    onToggleTheme = onToggleTheme,
                    darkTheme = darkTheme
                )
            }
            composable(Routes.STATS) {
                StatsScreen(onToggleTheme = onToggleTheme, darkTheme = darkTheme)
            }

            // ===== 子页面 =====
            composable(
                route = Routes.WORKOUT,
                arguments = listOf(
                    navArgument("planId") { defaultValue = -1L },
                    navArgument("dayId") { defaultValue = -1L },
                    navArgument("exerciseId") { defaultValue = -1L }
                )
            ) { entry ->
                val planId = entry.arguments?.getLong("planId")?.takeIf { it > 0 }
                val dayId = entry.arguments?.getLong("dayId")?.takeIf { it > 0 }
                val exerciseId = entry.arguments?.getLong("exerciseId")?.takeIf { it > 0 }
                WorkoutScreen(
                    planId = planId,
                    dayId = dayId,
                    initialExerciseId = exerciseId,
                    onExit = { navController.popBackStack() },
                    onOpenExercise = { navController.navigate(Routes.exercise(it)) }
                )
            }
            composable(
                route = Routes.EXERCISE,
                arguments = listOf(navArgument("id") { defaultValue = -1L })
            ) { entry ->
                ExerciseDetailScreen(
                    exerciseId = entry.arguments?.getLong("id") ?: -1L,
                    onBack = { navController.popBackStack() },
                    onStartWorkout = { id -> navController.navigate(Routes.workout(null, null, id)) }
                )
            }
            composable(
                route = Routes.PLAN,
                arguments = listOf(navArgument("planId") { defaultValue = -1L })
            ) { entry ->
                PlanDetailScreen(
                    planId = entry.arguments?.getLong("planId") ?: -1L,
                    onBack = { navController.popBackStack() },
                    onStartWorkout = { planId, dayId -> navController.navigate(Routes.workout(planId, dayId, null)) }
                )
            }
            composable(Routes.BODY) {
                BodyScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.PLANS) {
                PlanListScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPlan = { navController.navigate(Routes.plan(it)) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
