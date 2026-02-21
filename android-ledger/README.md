# Android Ledger

An automated, local-first finance tracker for expats, designed to perfectly segregate multiple currencies and profiles while ensuring no duplicate transactions.

## Setup Instructions

### Step 1: Install Necessary Tools
**Mac Users:**
```bash
# Install Homebrew
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
# Install Git
brew install git
# Download Android Studio: https://developer.android.com/studio
```

**Windows Users:**
- Install Git: Download from `https://git-scm.com/download/win`
- Install Android Studio: Download from `https://developer.android.com/studio`

### Step 2: Download Project
```bash
git clone https://github.com/yourusername/android-ledger.git
cd android-ledger
```

### Step 3: Build APK
**Mac/Linux:**
```bash
./scripts/build_apk.sh
```
**Windows:**
```powershell
scripts\build_apk.bat
```
Upon success, the APK is located at `artifacts/app-debug.apk`. 
*(Note: Android Studio will automatically download Gradle and required SDK components when you open the project or run the wrapper for the first time).*

### Step 4: Install to Phone
**Method A (USB Debugging):**
1. Connect via USB.
2. Enable USB Debugging in Developer Options.
3. Click "Run" in Android Studio.

**Method B (Wireless):**
1. Send `artifacts/app-debug.apk` to your phone.
2. Open the file and click Install (allow unknown sources if prompted).

## Google Play Release Note
- Target SDK is set to API 35.
- Privacy policy is hosted via `docs/privacy.md`.
- Ensure Notification Listener rationale is provided to users.
- Manage Secrets via GitHub repository settings to configure keystore via environment variables for `build_release_apk.sh`.
