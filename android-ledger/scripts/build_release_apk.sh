#!/bin/bash
echo "Building Release APK..."
cd ..
./gradlew assembleRelease
# Note: You still need to sign the APK manually or via keystore properties.
mkdir -p artifacts
cp app/build/outputs/apk/release/app-release-unsigned.apk artifacts/
echo "Build complete. Unsigned APK is in artifacts/"
