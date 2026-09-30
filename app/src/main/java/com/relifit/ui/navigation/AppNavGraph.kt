package com.relifit.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
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
import com.relifit.ui.plan.PlanDetailScreen
import com.relifit.ui.plan.PlanListScreen
import com.relifit.ui.settings.SettingsScreen
import com.relifit.ui.stats.StatsOverviewScreen
import com.relifit.ui.workout.WorkoutScreen
import com.relifit.ui.workoutHub.WorkoutHubScreen

/** 底部导航 3 大主界面（首页、饮食、锻炼） */
private data class NavItem(val route: String, val label: String, val icon: ImageVector)

private val navItems = listOf(
    NavItem(Routes.HOME_HUB, "首页", Icons.Filled.Home),
    NavItem(Routes.DIET, "饮食", Icons.Filled.Restaurant),
    NavItem(Routes.WORKOUT_HUB, "锻炼", Icons.Filled.FitnessCenter)
)

/**
 * 全局导航图：Scaffold + 底部导航（三大界面） + NavHost
 * 子页面（训练进行中/动作详情/计划详情/身体数据/设置）隐藏底部导航
 */
@Composable
fun AppNavGraph(darkTheme: Boolean, onToggleTheme: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in Routes.topLevel

    /**
     * 统一的顶部 Tab 导航
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
            startDestination = Routes.HOME_HUB,
            modifier = Modifier.padding(padding)
        ) {
            // ===== 1. 首页主界面（今日安排 + 数据统计整合） =====
            composable(Routes.HOME_HUB) {
                StatsOverviewScreen(
                    onOpenPlan = { navController.navigate(Routes.plan(it)) },
                    onOpenPlans = { navController.navigate(Routes.PLANS) },
                    onStartWorkout = { planId, dayId -> navController.navigate(Routes.workout(planId, dayId, null)) },
                    onOpenBody = { navController.navigate(Routes.BODY) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onToggleTheme = onToggleTheme,
                    darkTheme = darkTheme
                )
            }

            // ===== 2. 饮食主界面 =====
            composable(Routes.DIET) {
                DietScreen(
                    onToggleTheme = onToggleTheme,
                    darkTheme = darkTheme
                )
            }

            // ===== 3. 锻炼主界面（动作 + 记录整合） =====
            composable(Routes.WORKOUT_HUB) {
                WorkoutHubScreen(
                    onStartWorkout = { planId, dayId, exerciseId ->
                        navController.navigate(Routes.workout(planId, dayId, exerciseId))
                    },
                    onOpenExercise = { navController.navigate(Routes.exercise(it)) },
                    onOpenPlans = { navController.navigate(Routes.PLANS) },
                    onToggleTheme = onToggleTheme,
                    darkTheme = darkTheme
                )
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
