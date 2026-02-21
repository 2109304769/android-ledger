#!/bin/bash
set -e

echo "=== 构建 Debug APK ==="
echo ""

# 检查 JAVA_HOME
if [ -z "$JAVA_HOME" ]; then
    # 尝试自动检测 Android Studio 自带的 JDK
    if [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
        export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
    elif [ -d "$HOME/Library/Application Support/JetBrains/Toolbox/apps/android-studio/jbr" ]; then
        export JAVA_HOME="$HOME/Library/Application Support/JetBrains/Toolbox/apps/android-studio/jbr"
    elif [ -d "C:/Program Files/Android/Android Studio/jbr" ]; then
        export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
    else
        echo "错误：未找到 JAVA_HOME。请安装 Android Studio 或手动设置 JAVA_HOME。"
        exit 1
    fi
fi

echo "使用 JAVA_HOME: $JAVA_HOME"
echo ""

# 进入项目目录
cd "$(dirname "$0")/.."

# 构建
echo "开始构建..."
./gradlew assembleDebug

# 复制到 artifacts
mkdir -p artifacts
cp app/build/outputs/apk/debug/app-debug.apk artifacts/app-debug.apk

echo ""
echo "=== 构建完成！==="
echo "APK 路径: artifacts/app-debug.apk"
echo ""
echo "安装方式："
echo "  方式A（USB连接）: adb install artifacts/app-debug.apk"
echo "  方式B（发送文件）: 把 artifacts/app-debug.apk 通过微信/QQ/邮件发到手机"
