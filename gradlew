# 锐炼Fit Gradle Wrapper（Unix/macOS，Windows 使用 gradlew.bat）
# 需要 gradle/wrapper/gradle-wrapper.jar；Android Studio 同步不依赖此文件。

APP_NAME="Gradle"
APP_BASE_NAME=${0##*/}
DIRNAME=$(cd "$(dirname "$0")" && pwd)
APP_HOME=$DIRNAME
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

exec "$JAVA_HOME/bin/java" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
  "-Dorg.gradle.appname=$APP_BASE_NAME" -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
  org.gradle.wrapper.GradleWrapperMain "$@"
