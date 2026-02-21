#!/bin/bash
echo "Building Debug APK..."
cd ..
./gradlew assembleDebug
mkdir -p artifacts
cp app/build/outputs/apk/debug/app-debug.apk artifacts/
echo "Build complete. APK is in artifacts/app-debug.apk"
