# 锐炼Fit Gradle Wrapper（Unix/macOS，Windows 使用 gradlew.bat）
# 需要 gradle/wrapper/gradle-wrapper.jar；Android Studio 同步不依赖此文件。

APP_NAME="Gradle"
APP_BASE_NAME=${0##*/}
DIRNAME=$(cd "$(dirname "$0")" && pwd)
APP_HOME=$DIRNAME
DEFAULT_JVM_OPTS="-Xmx64m -Xms64m"

if [ -z "$JAVA_HOME" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
    if [ -x "/opt/homebrew/opt/openjdk@17/bin/java" ]; then
        export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
    elif [ -x "/usr/libexec/java_home" ]; then
        export JAVA_HOME="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
    fi
fi

JAVACMD="${JAVA_HOME:+$JAVA_HOME/bin/java}"
JAVACMD="${JAVACMD:-java}"

exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
  "-Dorg.gradle.appname=$APP_BASE_NAME" -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
  org.gradle.wrapper.GradleWrapperMain "$@"
