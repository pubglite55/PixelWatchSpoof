# 绑定流程技术文档

## 概述

本文档详细描述小米运动健康 App 的设备绑定流程，以及 PixelWatchSpoof 如何拦截和伪造各环节。

## 正常绑定流程

```
用户点击"添加设备"
        ↓
BLE 扫描发现设备
        ↓
用户选择设备
        ↓
创建 BT 配对 (createBond)
        ↓
建立 SPP 连接 (BluetoothSocket.connect)
        ↓
SPP 版本握手 (y8.m.o)
        ↓
发送 getBindInfo 请求 (WearBinderV2.i)
        ↓
服务器返回认证数据
        ↓
根据 verifyMode 选择认证方式:
  ├── verifyMode=2 → LocalWearBinderV2 (本地认证)
  └── verifyMode=1 → PSKWearBinderV2 (PSK认证)
        ↓
执行认证流程
        ↓
调用 onBindSuccess(model, did, mac, sn)
        ↓
requestDevice() → 服务器获取设备列表
        ↓
notifyDeviceBind() → 注册设备
        ↓
connectDevice() → BLE GATT 连接
        ↓
绑定完成，跳转主页
```

## 伪造流程

```
用户点击"添加设备"
        ↓
BLE 扫描发现设备 ← ScanHook 伪装广播包
        ↓
用户选择设备
        ↓
创建 BT 配对 ← BondHook 返回 BOND_BONDED
        ↓
建立 SPP 连接 ← BondHook 拦截 connect()
        ↓
SPP 版本握手 ← BondHook 伪造 version 2.0
        ↓
发送 getBindInfo 请求 ← SppAuthHook 拦截
        ↓
直接调用 onBindSuccess ← 跳过服务器调用
        ↓
requestDevice() ← SppAuthHook 拦截
  ├── notifyDeviceBind() → 注册设备
  └── 跳过 onRequestDeviceSuccess (避免 GATT 崩溃)
        ↓
App 尝试 BLE GATT 连接 ← 待修复
        ↓
Activity 卡住 ← 待修复
```

## 关键 Hook 点

### 1. SppAuthHook — 核心绑定劫持

**目标类**: `com.xiaomi.fit.device.bind.WearBinderV2`
**目标方法**: `i()` (无参，getBindInfo)

```kotlin
// 拦截 getBindInfo，直接调用 onBindSuccess
module.hook(method).intercept { chain ->
    val binder = chain.thisObject
    // WearBinderV2 的父类 'a' 有 mCallback 字段
    val cbField = findField(binder, "mCallback")
    val target = cbField.get(binder)  // DualCoreDeviceBinder 实例
    
    // 调用 onBindSuccess(model, did, mac, sn)
    val onSuccessMethod = target.javaClass.methods.firstOrNull {
        it.name == "onBindSuccess" && it.parameterTypes.size == 4
    }
    onSuccessMethod?.invoke(target, "midr.watch.m62s", "pixel_watch_035t", "XX:XX:XX:XX:5B:85", null)
    return@intercept null
}
```

### 2. BondHook — BT 配对伪造

**目标类**: `android.bluetooth.BluetoothDevice`
**目标方法**: `getBondState()`, `createBond()`

```kotlin
// getBondState 返回已配对
module.hook(getBondStateMethod).intercept { chain ->
    val device = chain.thisObject
    val address = device.getAddress()
    if (address.contains("5B:85")) {
        return@intercept 12  // BOND_BONDED
    }
    chain.proceed()
}
```

### 3. BondHook — SPP 连接伪造

**目标类**: `y8.m` (LyraWOS SppClient)
**目标方法**: `o()` (静态方法，3个参数)

```kotlin
// 伪造 SPP 版本握手
module.hook(method).intercept { chain ->
    val sppClient = chain.args[0]
    // 设置 isConnected = true
    // 设置 mVersion = 2
    // 设置 mVersionName = "2.0"
    // 调用 notifyConnectSuccess()
    return@intercept null
}
```

### 4. SppAuthHook — requestDevice 拦截

**目标类**: `com.xiaomi.fit.device.bind.BaseDeviceBinder`
**目标方法**: `requestDevice(String)`

```kotlin
// 跳过服务器调用，直接注册设备
module.hook(method).intercept { chain ->
    val binder = chain.thisObject
    val did = findField(binder, "mDid")?.get(binder)
    
    // 调用 notifyDeviceBind
    val mgr = getMDeviceManager(binder)
    mgr.notifyDeviceBind(did, false)
    
    // 跳过 onRequestDeviceSuccess (避免触发 GATT 连接)
    return@intercept null
}
```

## 进程模型

小米运动健康 App 使用多进程架构：

| 进程 | 作用 | Hook 范围 |
|------|------|-----------|
| `com.mi.health` | 主进程，UI 界面 | ScanHook, ProductHook |
| `com.mi.health:device` | 设备管理服务 | SppAuthHook, BondHook |
| `com.xiaomi.mi_connect_service` | LyraWOS 连接服务 | BondHook (y8.m.o) |
| `com.android.bluetooth` | 蓝牙系统服务 | BluetoothHook |

**重要**: 跨进程的静态字段不共享。`bindActivityRef` 在 `:device` 进程中为 null。

## 关键发现

### 类继承关系

```
WearBinderV2 extends a (抽象基类)
  └── mCallback 字段在 'a' 中，类型为 fs2 (BaseDeviceBinder)

BaseDeviceBinder implements fs2
├── BluetoothDeviceBinder
│   └── DualCoreDeviceBinder (实际使用)
└── callback 字段 (UI 回调，AIDL 接口)
```

### 混淆名称映射

| 混淆名 | 原始名 | 位置 |
|--------|--------|------|
| `a` | 抽象绑定器基类 | `com.xiaomi.fit.device.bind.a` |
| `fs2` | 绑定回调接口 | `BaseDeviceBinder` 实现 |
| `y8.m.o()` | SPP 连接方法 | LyraWOS SppClient |
| `y8.d` | SppClient 类 | LyraWOS |

### 已知限制

1. **服务器端注册未完成**: `applyBind`/`confirmBind` 未伪造，设备绑定不持久
2. **GATT 连接缺失**: `connectDevice` → `openBluetooth` 失败
3. **Activity 卡住**: 等待 GATT 连接完成，永远无法超时
4. **重启后丢失**: 内存中的设备绑定在 force-stop 后丢失

## 调试技巧

### 添加诊断日志

```kotlin
module.log(Log.INFO, TAG, "关键变量: $variableName")
module.log(Log.WARN, TAG, "异常信息: ${e.message}")
```

### 查看方法签名

```bash
# 使用 jadx 反编译
jadx -d /tmp/decompiled target.apk

# 查看类的方法
grep -n "methodName" /tmp/decompiled/sources/ClassName.java
```

### 追踪调用栈

```kotlin
module.hook(method).intercept { chain ->
    val stackTrace = Thread.currentThread().stackTrace
    stackTrace.take(10).forEach { 
        module.log(Log.INFO, TAG, "  at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})")
    }
    chain.proceed()
}
```
