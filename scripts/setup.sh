#!/bin/bash
set -e

echo "=== 环境检查 ==="
echo ""

# 检查 Java
if command -v java &> /dev/null; then
    echo "✅ Java 已安装:"
    java -version 2>&1 | head -1
else
    echo "❌ Java 未找到"
    echo "   请安装 Android Studio，它会自带 Java。"
    echo "   下载地址: https://developer.android.com/studio"
    exit 1
fi

# 检查 ANDROID_HOME / ANDROID_SDK_ROOT
if [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME" ]; then
    echo "✅ Android SDK: $ANDROID_HOME"
elif [ -n "$ANDROID_SDK_ROOT" ] && [ -d "$ANDROID_SDK_ROOT" ]; then
    echo "✅ Android SDK: $ANDROID_SDK_ROOT"
elif [ -d "$HOME/Library/Android/sdk" ]; then
    echo "✅ Android SDK: $HOME/Library/Android/sdk (auto-detected)"
elif [ -d "$LOCALAPPDATA/Android/Sdk" ]; then
    echo "✅ Android SDK: $LOCALAPPDATA/Android/Sdk (auto-detected)"
else
    echo "❌ Android SDK 未找到"
    echo "   请安装 Android Studio 并让它下载 SDK。"
    echo "   下载地址: https://developer.android.com/studio"
    exit 1
fi

# 检查 Gradle Wrapper
cd "$(dirname "$0")/.."
if [ -f "gradlew" ]; then
    echo "✅ Gradle Wrapper 存在"
else
    echo "❌ Gradle Wrapper 缺失"
    exit 1
fi

echo ""
echo "=== 环境检查通过！==="
echo "可以运行 scripts/build_apk.sh 构建 APK 了。"
