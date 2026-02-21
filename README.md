# 记账本 — Android 多币种记账 App

一款支持多账户、多币种（EUR/CNY）的本地记账应用，专为在欧洲生活的华人设计。

## 功能特点

- **多账户管理**：个人账户、公司账户分开记
- **多币种支持**：EUR 和 CNY 钱包，自动汇率换算
- **三层结构**：账户 → 钱包 → 来源（Revolut / Wise / 微信 / 现金等）
- **CSV 导入**：支持 Revolut、Wise、Poste Italiane 账单导入
- **通知监听**：自动抓取微信/支付宝支付通知记账（可选）
- **完全本地**：所有数据保存在手机本地，不上传任何服务器

## 技术栈

- Jetpack Compose + Material 3
- Room 数据库
- Hilt 依赖注入
- Kotlin + Coroutines + Flow

---

## 快速开始

### Step 1：安装必要工具（只需做一次）

**Windows 用户：**
1. 安装 Git：去 https://git-scm.com/download/win 下载安装包，一路点 Next
2. 安装 Android Studio：去 https://developer.android.com/studio 下载，一路点 Next
3. 打开 Android Studio，让它自动下载 Android SDK（可能需要 10-30 分钟）

**Mac 用户：**
```bash
# 安装 Homebrew（如果没有）
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 安装 Git
brew install git

# 下载 Android Studio
# 去这个网址下载：https://developer.android.com/studio
# 下载后双击 .dmg 文件，把 Android Studio 拖到 Applications 文件夹
```

### Step 2：下载项目
```bash
git clone https://github.com/你的用户名/android-ledger.git
cd android-ledger
```

### Step 3：构建 APK
```bash
# Mac/Linux：
./scripts/build_apk.sh

# Windows：
scripts\build_apk.bat
```
构建成功后，APK 在这里：`artifacts/app-debug.apk`

### Step 4：安装到手机

**方法 A（推荐，用数据线）：**
1. 手机用数据线连电脑
2. 手机上开启"USB 调试"（在开发者选项里，不同手机搜索自己型号）
3. 在 Android Studio 里点顶部绿色三角形运行按钮

**方法 B（无线，发文件）：**
1. 把 `artifacts/app-debug.apk` 通过微信/QQ/AirDrop 发到手机
2. 手机上打开文件，点击安装
3. 如果提示"来源不明的应用"，在设置里允许安装即可

---

## 构建 Release APK（上架用）

```bash
# Mac/Linux：
./scripts/build_release_apk.sh

# 首次运行会引导你创建签名文件
# ⚠️ 签名文件和密码请妥善保管，丢失后无法更新 App！
```

### GitHub Actions 自动构建

每次推送到 main 分支会自动构建 Debug APK。在 GitHub Actions 的 Artifacts 中可以下载。

如需自动构建 Release APK，需要在 GitHub 仓库的 Settings → Secrets 中添加 `KEYSTORE_PASSWORD`。

---

## 上架 Google Play 注意事项

1. **targetSdk** 必须是最新版本（已配置为 35）
2. **隐私政策**：`docs/privacy.md`，上架时填写链接（可用 GitHub Pages 托管）
3. **通知监听权限**：上架时需提交使用说明，说明用于帮助用户自动记账，所有数据本地处理
4. **Release APK 必须签名**：用 `build_release_apk.sh` 生成，keystore 文件妥善保管

---

## 项目结构

```
android-ledger/
├── app/src/main/java/com/androidledger/
│   ├── ui/              # 界面层（Compose）
│   ├── data/            # 数据层（Room entities, DAOs, repositories）
│   ├── domain/          # 业务逻辑层
│   ├── integration/     # 外部集成（CSV导入、通知监听）
│   └── di/              # Hilt 依赖注入
├── docs/                # 文档
├── scripts/             # 构建脚本
└── .github/workflows/   # CI 自动构建
```

## 开发路线图

详见 [docs/roadmap.md](docs/roadmap.md)

## 隐私政策

详见 [docs/privacy.md](docs/privacy.md)
