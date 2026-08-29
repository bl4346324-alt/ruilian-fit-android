plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.relifit"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.relifit"
        minSdk = 26          // Android 8.0（PRD 强制约束）
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }
    }

    // ===== 签名：独立 release keystore（app/release.keystore），不再用公开的 debug 签名 =====
    // 密码为个人测试用途；正式上架前请更换为私有 keystore 并妥善保管（勿提交到仓库）
    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = "relifit123"
            keyAlias = "relifit"
            keyPassword = "relifit123"
        }
    }

    buildTypes {
        release {
            // 上架前可开启 isMinifyEnabled = true 启用 R8 混淆（Compose 需配合 keep 规则）
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    // Kotlin 1.9.22 对应的 Compose 编译器版本（1.5.10）
    composeOptions { kotlinCompilerExtensionVersion = "1.5.10" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    // 开发阶段跳过 release 的 lint 强制检查（lint 仅为静态提示，不影响功能）
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    // ===== 基础 =====
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)

    // ===== Compose (BOM 统一版本) =====
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.activity.compose)

    // ===== Navigation-Compose 路由 =====
    implementation(libs.androidx.navigation.compose)

    // ===== Room 本地数据库 =====
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ===== DataStore 持久化主题/设置 =====
    implementation(libs.androidx.datastore.preferences)

    debugImplementation(libs.androidx.ui.tooling)
}
