# 锐炼Fit（ReliFit）

一款**纯本地、无网络、无登录**的 Android 健身训练记录 App。
覆盖动作库、训练计划、训练进行中计时记录、饮食热量、身体数据、数据统计与设置，所有数据仅存储在本机。

> 包名：`com.relifit` ｜ 应用名：锐炼Fit ｜ 当前版本：1.0.0

---

## ✨ 功能特性

### 底部 5 大 Tab

| Tab          | 功能                                                         |
| ------------ | ------------------------------------------------------------ |
| **首页**     | 今日训练 Hero 卡片（周进度环）、当前计划环卡、身体数据摘要   |
| **动作库**   | 47 个动作、六大肌群（胸/背/肩/腿/手臂/核心）筛选、名称/英文/肌群/器械模糊搜索、收藏 |
| **训练记录** | 历史训练列表（可折叠查看每组详情）、按日期筛选、删除         |
| **饮食记录** | 每日四餐（早/午/晚/加餐）、内置食物营养数据库自动匹配、份数 +/− 调整、热量与三大营养素目标 |
| **数据统计** | 周/月报表（次数/时长/容量）、训练频率柱状图、肌群覆盖分布、单动作重量进步折线图 |

### 核心训练能力

- **训练进行中**：动作/组会话状态机、总计时（退后台自动暂停）、组间休息倒计时（+30s / 跳过 / 震动提醒）、重量次数步进（kg 步进 2.5 / lb 步进 5）、智能预填上次重量
- **训练计划**：7 套内置模板（力量提升、新手增肌、减脂燃脂、居家无器械、核心强化、有氧燃脂、恢复放松），模板只读、一键复制生成自定义计划；训练日与动作条目的增删改
- **周期进度回绕**：计划周期超过后自动回绕新一轮（4 周计划练到第 5 周显示 1/4）
- **事务化保存**：一次训练（日志 + 全部组）与复制模板（计划 + 训练日 + 动作）均为原子提交，不留孤儿数据

### 系统与数据

- **主题**：浅色 / 深色 / 跟随系统，顶栏一键切换
- **单位切换**：kg / lb（数据库以 kg 存储，展示层换算）
- **身体数据**：体重 / 身高 / 每日运动量，同一天记录自动覆盖
- **系统备份**：支持系统备份训练数据库与设置（Android 8.0+）

---

## 🛠 技术栈

| 分类       | 选型                                                         |
| ---------- | ------------------------------------------------------------ |
| 语言       | Kotlin 1.9.22（Java 17）                                     |
| UI         | Jetpack Compose（BOM 2024.02.01）+ Material 3                |
| 架构       | MVVM：单 Activity + Compose Navigation + ViewModel + Repository |
| 本地数据库 | Room 2.6.1（KSP 编译），10 张表                              |
| 设置持久化 | DataStore Preferences 1.0.0                                  |
| 依赖注入   | 手动 DI（`ReliFitApp` 全局容器，无 Hilt，遵循"最少第三方库"约束） |
| 其他       | Kotlin Coroutines 1.7.3、Navigation Compose 2.7.7            |

## 📁 项目结构

```
app/src/main/java/com/relifit/
├── MainActivity.kt            # 唯一 Activity：承载 Compose 导航图 + 主题切换
├── ReliFitApp.kt              # Application：手动 DI 容器 + 种子数据初始化
├── data/
│   ├── local/                 # Room 数据库、DAO、实体、种子数据（动作库/模板/食物营养）
│   └── repository/            # 6 个 Repository（Workout/Plan/Exercise/Body/Diet/Settings）
├── ui/
│   ├── navigation/            # 路由常量 + AppNavGraph（底部导航）
│   ├── theme/                 # Material3 主题（深浅色定制配色）
│   ├── components/            # 通用组件（卡片/图表/顶栏等）
│   ├── home/  library/  plan/  workout/  logs/  diet/
│   ├── body/  stats/  settings/  exerciseDetail/
│   └── ...                    # 每个模块 = Screen + ViewModel
└── util/                      # 时间工具（周/月区间）、单位换算
```

## 🚀 快速开始

### 环境要求

- Android Studio（建议最新稳定版，项目使用 AGP 8.2.2）
- JDK 17
- Android SDK：`compileSdk 34`、`minSdk 26`（Android 8.0）、`targetSdk 34`

### 构建运行

1. `git clone` 本项目并用 **Android Studio** 打开（首次同步会自动下载 Gradle 8.5 与依赖，国内环境已配置阿里云镜像加速）
2. 等待 Gradle Sync 完成
3. 连接设备或模拟器，点击 **Run** ▶️

> 首次启动会自动写入种子数据：47 个动作 + 7 套训练模板；饮食页内置食物营养数据库开箱即用。

### 构建 APK

- Debug 包：`Build > Build Bundle(s) / APK(s) > Build APK(s)`
- Release 包：使用独立签名 `app/release.keystore`（**请勿将 keystore 提交到仓库**，`.gitignore` 已忽略 `*.keystore`；正式上架前请更换为私有密钥）

## 📊 数据说明

- 全部数据仅存储本机（Room 数据库 `relifit.db` + DataStore `relifit_settings`），**无任何网络请求、无账号体系**
- 开发阶段数据库升级使用破坏性迁移（升级会清空本地数据）；正式发布前应补充 Migration 保留用户数据
- 设置页提供"清除全部本地数据并恢复默认内容"入口

## 📋 版本记录

- **1.0.0**：首个版本，覆盖 PRD P0 全部核心模块（动作库 / 计划 / 训练 / 记录 / 饮食 / 身体数据 / 统计 / 设置）

## 📄 许可证

本项目为个人学习 / 演示用途，暂无开源许可证。
