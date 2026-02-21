@echo off
chcp 65001 >nul 2>&1
echo === 构建 Debug APK ===
echo.

:: 检查 JAVA_HOME
if "%JAVA_HOME%"=="" (
    if exist "C:\Program Files\Android\Android Studio\jbr" (
        set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
    ) else (
        echo 错误：未找到 JAVA_HOME。请安装 Android Studio 或手动设置 JAVA_HOME。
        exit /b 1
    )
)

echo 使用 JAVA_HOME: %JAVA_HOME%
echo.

:: 进入项目目录
cd /d "%~dp0\.."

:: 构建
echo 开始构建...
call gradlew.bat assembleDebug
if %ERRORLEVEL% neq 0 (
    echo 构建失败！
    exit /b 1
)

:: 复制到 artifacts
if not exist artifacts mkdir artifacts
copy /y "app\build\outputs\apk\debug\app-debug.apk" "artifacts\app-debug.apk" >nul

echo.
echo === 构建完成！===
echo APK 路径: artifacts\app-debug.apk
echo.
echo 安装方式：
echo   方式A（USB连接）: adb install artifacts\app-debug.apk
echo   方式B（发送文件）: 把 artifacts\app-debug.apk 通过微信/QQ/邮件发到手机
