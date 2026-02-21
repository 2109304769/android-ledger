@echo off
echo Building Debug APK...
cd ..
call gradlew.bat assembleDebug
if not exist artifacts mkdir artifacts
copy app\build\outputs\apk\debug\app-debug.apk artifacts\
echo Build complete. APK is in artifacts/app-debug.apk
