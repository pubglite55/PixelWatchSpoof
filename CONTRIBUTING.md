# 贡献指南

感谢你对 PixelWatchSpoof 项目的关注！

## 如何贡献

### 报告问题

1. 搜索现有 Issue 确认未被报告
2. 创建新 Issue，包含：
   - 设备型号和 Android 版本
   - 小米运动健康 App 版本
   - LSPosed 版本
   - 详细的复现步骤
   - 相关日志（`adb logcat | grep SppAuthHook`）

### 提交代码

1. Fork 本仓库
2. 创建特性分支：`git checkout -b feature/your-feature`
3. 提交更改：`git commit -m 'feat: 添加某某功能'`
4. 推送到分支：`git push origin feature/your-feature`
5. 创建 Pull Request

### 代码规范

- 使用 Kotlin 编写新代码
- 遵循现有的代码风格
- 为新功能添加注释
- 确保构建通过：`./gradlew assembleDebug`

### 提交信息格式

```
feat: 新功能
fix: 修复 bug
docs: 文档更新
style: 代码格式调整
refactor: 重构
test: 添加测试
chore: 构建/工具变更
```

## 开发环境搭建

### 前置条件

- JDK 21+
- Android SDK (API 35)
- Android Studio 或命令行工具
- LSPosed/Xposed 框架（测试用）

### 构建步骤

```bash
# 克隆仓库
git clone https://github.com/your-username/PixelWatchSpoof.git
cd PixelWatchSpoof

# 设置 JAVA_HOME
export JAVA_HOME=/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home

# 构建 Debug APK
./gradlew assembleDebug
```

### 测试流程

1. 安装 APK 到设备
2. 在 LSPosed 中启用模块
3. 设置作用域
4. 强制停止目标 App
5. 重新打开 App 进行测试
6. 查看日志：`adb logcat | grep SppAuthHook`

## 项目结构

```
app/src/main/java/io/github/pixelwatchspoof/
├── HookEntry.java          # LSPosed 入口
├── MainActivity.java       # 模块设置
├── config/
│   └── DeviceConfig.kt     # 设备配置
└── hooks/
    ├── SppAuthHook.kt      # SPP 认证 + 绑定流程
    ├── BondHook.kt         # BT 配对 + SPP 连接
    ├── ScanHook.kt         # BLE 扫描伪装
    └── ...                 # 其他 hook 模块
```

## 逆向分析

如果你要添加新的 hook：

1. 使用 jadx 反编译目标 APK
2. 找到需要 hook 的类和方法
3. 在对应的 Hook 文件中添加 hook 代码
4. 使用 `module.log()` 记录关键信息
5. 测试并验证 hook 生效

## 行为准则

- 尊重其他贡献者
- 专注于技术讨论
- 不发布有害或恶意内容
- 遵守项目许可证
