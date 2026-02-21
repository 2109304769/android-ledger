#!/bin/bash
set -e

echo "=== 构建 Release APK ==="
echo ""

# 检查 JAVA_HOME
if [ -z "$JAVA_HOME" ]; then
    if [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
        export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
    elif [ -d "C:/Program Files/Android/Android Studio/jbr" ]; then
        export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
    else
        echo "错误：未找到 JAVA_HOME。请安装 Android Studio 或手动设置 JAVA_HOME。"
        exit 1
    fi
fi

cd "$(dirname "$0")/.."

# 检查 keystore
KEYSTORE_FILE="release-keystore.jks"
if [ ! -f "$KEYSTORE_FILE" ]; then
    echo "未找到签名文件 ($KEYSTORE_FILE)"
    echo "首次发布需要创建签名文件。请按提示操作："
    echo ""
    read -p "请输入密钥库密码 (至少6位): " -s KEYSTORE_PASSWORD
    echo ""
    read -p "请输入密钥密码 (至少6位): " -s KEY_PASSWORD
    echo ""
    read -p "请输入您的姓名: " CN_NAME

    keytool -genkeypair \
        -v \
        -keystore "$KEYSTORE_FILE" \
        -alias "android-ledger" \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -storepass "$KEYSTORE_PASSWORD" \
        -keypass "$KEY_PASSWORD" \
        -dname "CN=$CN_NAME"

    echo ""
    echo "签名文件已创建: $KEYSTORE_FILE"
    echo "⚠️  请妥善保管此文件和密码，丢失后无法更新 App！"
    echo ""
fi

if [ -z "$KEYSTORE_PASSWORD" ]; then
    read -p "请输入密钥库密码: " -s KEYSTORE_PASSWORD
    echo ""
fi
if [ -z "$KEY_PASSWORD" ]; then
    KEY_PASSWORD="$KEYSTORE_PASSWORD"
fi

# 构建 Release APK
./gradlew assembleRelease \
    -Pandroid.injected.signing.store.file="$(pwd)/$KEYSTORE_FILE" \
    -Pandroid.injected.signing.store.password="$KEYSTORE_PASSWORD" \
    -Pandroid.injected.signing.key.alias="android-ledger" \
    -Pandroid.injected.signing.key.password="$KEY_PASSWORD"

mkdir -p artifacts
cp app/build/outputs/apk/release/app-release.apk artifacts/app-release.apk

echo ""
echo "=== Release APK 构建完成！==="
echo "APK 路径: artifacts/app-release.apk"
