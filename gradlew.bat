@rem
@rem 锐炼Fit Gradle Wrapper 启动脚本（Windows）
@rem 需要 gradle/wrapper/gradle-wrapper.jar；若缺失，可先让 Android Studio 同步一次，
@rem 或在项目根目录执行已安装的 gradle wrapper 生成。
@rem
@if "%DEBUG%"=="" @echo off
@rem 定位 APP_HOME
setlocal
set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem 使用默认 JVM 参数
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

set WRAPPER_JAR=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
if not exist "%WRAPPER_JAR%" (
    echo ERROR: gradle-wrapper.jar not found at "%WRAPPER_JAR%"
    echo Android Studio 的 Gradle 同步不依赖此文件；如需命令行构建请先生成 wrapper。
    exit /b 1
)

"%JAVA_HOME%\bin\java.exe" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
endlocal
