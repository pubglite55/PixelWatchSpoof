# PixelWatchSpoof

> **Disclaimer**: This project is for educational and research purposes only. Use at your own risk. The author is not responsible for any damage or policy violations.

将 Pixel Watch 1 (Google Wear OS) 伪装为小米手表 5，欺骗小米运动健康 App 完成设备绑定流程。

## 项目概述

本项目是一个 LSPosed/Xposed 模块，通过 hook 小米运动健康 App（`com.mi.health`）的内部绑定流程，将真实的 Google Pixel Watch 1 伪装成小米手表 5，使 App 误认为正在绑定一台兼容的小米设备。

### 为什么需要这个？

小米运动健康 App 只支持绑定小米/红米品牌的可穿戴设备。Google Pixel Watch 1 虽然运行 Wear OS，但无法直接在小米运动健康中绑定。本模块通过以下方式绕过限制：

1. **BLE 扫描伪装** — 将 Pixel Watch 的广播包伪装为小米手表的格式
2. **产品校验绕过** — 跳过设备型号验证
3. **SPP 认证伪造** — 伪造串口协议认证响应
4. **绑定流程劫持** — 直接调用绑定成功回调

## 功能状态

| 功能 | 状态 | 说明 |
|------|------|------|
| BLE 扫描过滤绕过 | ✅ 完成 | Pixel Watch 出现在扫描列表 |
| 产品校验绕过 | ✅ 完成 | 识别为小米手表 5 |
| BT 配对伪造 | ✅ 完成 | `getBondState()` 返回已配对 |
| SPP 连接伪造 | ✅ 完成 | `y8.m.o()` 拦截成功 |
| `getBindInfo` 拦截 | ✅ 完成 | 直接调用 `onBindSuccess` |
| `onBindSuccess` 处理 | ✅ 完成 | 设置内部状态、回调通知 |
| `connectDevice` 拦截 | ✅ 完成 | 跳过 GATT 连接尝试 |
| `onBindFailure` 重定向 | ✅ 完成 | 失败回调转为成功 |
| Activity 转换 | ⏳ 进行中 | 绑定完成后自动跳转主页 |

## 系统要求

- **Android 版本**: 8.0+ (API 26+)
- **LSPosed/Xposed 框架**: 已安装并运行
- **目标 App**: 小米运动健康 (`com.mi.health`) v3.56.0+
- **Root 权限**: 需要（LSPosed 需要）

## 安装方法

### 方法一：直接安装 APK

1. 从 [Releases](https://github.com/pubglite55/PixelWatchSpoof/releases) 下载最新 APK
2. 安装 APK
3. 打开 LSPosed 管理器
4. 启用 PixelWatchSpoof 模块
5. 设置作用域为 `com.mi.health`、`com.xiaomi.mi_connect_service`、`com.android.bluetooth`
6. 强制停止小米运动健康 App
7. 重新打开 App，进入添加设备流程

### 方法二：从源码构建

```bash
# 克隆仓库
git clone https://github.com/pubglite55/PixelWatchSpoof.git
cd PixelWatchSpoof

# 设置 JAVA_HOME
export JAVA_HOME=/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home

# 构建 Debug APK
./gradlew assembleDebug

# APK 输出位置
# app/build/outputs/apk/debug/app-debug.apk
```

## 技术架构

```
PixelWatchSpoof/
├── app/src/main/java/io/github/pixelwatchspoof/
│   ├── HookEntry.java          # LSPosed 入口，注册所有 hook
│   ├── MainActivity.java       # 模块设置界面
│   ├── config/
│   │   └── DeviceConfig.kt     # 设备配置（型号、MAC 地址等）
│   └── hooks/
│       ├── SppAuthHook.kt      # 🔑 核心：SPP 认证伪造 + 绑定流程劫持
│       ├── BondHook.kt         # BT 配对伪造 + SPP 连接伪造
│       ├── ScanHook.kt         # BLE 扫描伪装
│       ├── ProductHook.kt      # 产品校验绕过
│       ├── BluetoothHook.kt    # 蓝牙扫描过滤绕过
│       ├── DeviceListHook.kt   # 设备列表注入
│       ├── DeviceInfoHook.kt   # 设备信息伪装
│       ├── BindHook.kt         # 绑定服务端参数替换
│       ├── AuthHook.kt         # 认证流程 hook
│       ├── BypassHook.kt       # 各种绕过 hook
│       ├── DataInjectHook.kt   # BLE 数据注入
│       ├── TransportHook.kt    # 传输层 hook
│       └── WearOsHook.kt       # Wear OS 相关 hook
├── docs/
│   └── BIND_FLOW.md            # 绑定流程技术文档
├── scope.list                  # LSPosed 作用域列表
└── build.gradle.kts            # 构建配置
```

## 技术详解

### 1. BLE 扫描伪装 (`ScanHook.kt`)

小米运动健康 App 通过 BLE 扫描发现附近的可穿戴设备。本模块 hook 了 `ScanFilter.matches()` 方法，将 Pixel Watch 1 的广播包伪装为小米手表的格式。

**关键修改：**
- 注入小米公司 ID (0x038F) 的制造商数据
- 修改广播包中的设备名称
- 伪造设备型号信息

### 2. SPP 认证伪造 (`SppAuthHook.kt`)

这是项目的核心模块，解决了绑定流程中最关键的认证环节。

**绑定流程分析：**

```
App 启动绑定
    ↓
BLE 扫描发现设备 ← ScanHook 拦截
    ↓
创建 BT 配对 ← BondHook 伪造
    ↓
建立 SPP 连接 ← BondHook 伪造 (y8.m.o)
    ↓
发送 getBindInfo 请求 ← SppAuthHook 拦截
    ↓
直接调用 onBindSuccess ← 跳过服务器调用
    ↓
设置内部状态 + 通知 UI
    ↓
跳过 GATT 连接 ← connectDevice 拦截
    ↓
完成绑定
```

### 3. 绑定成功处理 (`hookBindSuccess`)

拦截 `BaseDeviceBinder.onBindSuccess()` 方法，执行以下操作：

1. **设置内部状态**：`mDid`、`sn`、`isBindSuccess`
2. **移除设备绑定器**：从 `DeviceFactory` 中移除
3. **报告绑定成功**：调用 `reportBindSuccess()`
4. **设置偏好**：关闭新绑定模式
5. **移除设备 ID**：从账户中移除
6. **通知 UI**：调用 `callback.onBindSuccess(did)`
7. **刷新设备列表**：调用 `callback.onRequestDeviceSuccess()`
8. **关闭绑定页面**：延迟 3 秒后启动主界面

### 4. GATT 连接拦截 (`hookConnectDevice`)

拦截 `DeviceManagerRemoteImpl.connectDevice()` 方法，跳过 BLE GATT 连接尝试。因为 Pixel Watch 1 不支持小米的 GATT 服务，连接会失败。

### 5. 绑定失败重定向 (`hookBindFailure`)

拦截 `BluetoothDeviceBinder.onBindFailure()` 方法，将绑定失败重定向为成功：
- 关闭未认证连接
- 设置绑定成功状态
- 调用 `callback.onBindSuccess(did)`
- 调用 `callback.onRequestDeviceSuccess()`

## 已知问题

### 1. 绑定后 Activity 卡住

**症状**：绑定完成后，Activity 停留在"正在连接设备..."界面。

**原因**：`requestDeviceList` 返回空列表（设备未在小米服务器注册），导致 ViewModel 的 `did` 为 null。

**当前解决**：在绑定成功后延迟 3 秒启动主界面，覆盖绑定页面。

### 2. 设备绑定不持久化

**症状**：强制停止 App 后重新打开，设备不在已绑定列表中。

**原因**：服务器端注册（`applyBind` → `confirmBind`）未完成，设备绑定只存在于内存中。

### 3. 设备名称显示错误

**症状**：绑定界面显示"Xiaomi Watch S2 46mm"而非预期的"小米手表 5"。

**原因**：BLE 扫描结果中的产品信息匹配可能不正确。

## 调试方法

### 查看 LSPosed 日志

```bash
# 实时查看模块日志
adb logcat -s SppAuthHook:* BondHook:* ScanHook:* ProductHook:*

# 查看所有 PixelWatchSpoof 日志
adb logcat | grep -i "pixelwatchspoof\|SppAuthHook\|BondHook\|ScanHook"
```

### 关键日志标识

| 日志 | 含义 |
|------|------|
| `>>> getBindInfo intercepted` | WearBinderV2.i() 被拦截 |
| `Found mCallback: DualCoreDeviceBinder` | 成功找到回调对象 |
| `Calling onBindSuccess on binder` | 正在调用绑定成功 |
| `onBindSuccess intercepted` | 绑定成功被拦截处理 |
| `callback.onBindSuccess($did) called` | UI 回调已触发 |
| `Launched MainActivity` | 尝试关闭绑定页面 |

### 常用 adb 命令

```bash
# 强制停止 App
adb shell am force-stop com.mi.health

# 清除 App 数据（完全重置）
adb shell pm clear com.mi.health

# 启动 App
adb shell monkey -p com.mi.health -c android.intent.category.LAUNCHER 1

# 查看 App 进程
adb shell ps | grep mi.health

# 安装模块 APK
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 逆向工程笔记

### 关键类和方法

| 类/方法 | 作用 |
|---------|------|
| `WearBinderV2.i()` | getBindInfo — 发送绑定信息请求 |
| `BaseDeviceBinder.onBindSuccess()` | 绑定成功回调 |
| `BaseDeviceBinder.requestDevice()` | 请求设备列表 + 注册设备 |
| `DualCoreDeviceBinder` | 双核设备绑定器（实际使用的绑定器） |
| `IMiWearCoreClient.unauthCall()` | 未认证 IPC 调用 |
| `DeviceManagerRemoteImpl.connectDevice()` | BLE GATT 连接 |
| `BluetoothExtKt.openBluetooth()` | 打开蓝牙连接 |

### 类继承关系

```
BaseDeviceBinder (abstract)
├── BluetoothDeviceBinder
│   └── DualCoreDeviceBinder  ← 实际使用
└── (其他绑定器)

WearBinderV2 extends a (abstract)
├── LocalWearBinderV2
└── PSKWearBinderV2
```

### 混淆映射

| 混淆名 | 原始名 | 说明 |
|--------|--------|------|
| `a` | 抽象绑定器基类 | WearBinderV2 的父类 |
| `fs2` | 绑定回调接口 | `BaseDeviceBinder` 实现此接口 |
| `y8.m.o()` | SPP 连接方法 | LyraWOS 的 SppClient |
| `y8.d` | SppClient | LyraWOS 的 SPP 客户端类 |

## 构建环境

- **JDK**: OpenJDK 21
- **Android SDK**: API 35
- **Gradle**: 8.x
- **AGP**: 8.x
- **LSPosed API**: libxposed (新版 API)

## 贡献指南

欢迎贡献代码、报告问题或提出改进建议。

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

## 许可证

本项目采用 MIT 许可证 — 详见 [LICENSE](LICENSE) 文件。

## 致谢

- [LSPosed](https://github.com/LSPosed/LSPosed) — Xposed 框架
- [libxposed](https://github.com/libxposed) — 新版 Xposed API
- [jadx](https://github.com/skylot/jadx) — DEX 反编译工具
- 小米运动健康 App 逆向分析社区

## 免责声明

本项目仅供学习和研究用途。使用本模块可能违反小米运动健康 App 的服务条款。作者不对因使用本项目而产生的任何后果负责。请遵守当地法律法规。
