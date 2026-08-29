pluginManagement {
    repositories {
        // 国内加速镜像（阿里云），放在 google 之前优先命中
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // 阿里云镜像的 Gradle 插件门户仓库：提供 org.gradle:gradle 的 src 源码包
        // （Studio 同步 Kotlin DSL 时需要 gradle-8.5-src.zip，官方源在国内会超时）
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "ReliFit"
include(":app")
