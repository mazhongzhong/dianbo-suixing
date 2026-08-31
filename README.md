# 英伦随听 / UK Radio Companion

一个面向海外收听场景的简洁 Android 英国广播播放器。目前支持 BBC World Service、Radio 1、Radio 2、Radio 3、Radio 4、Radio 5 Live 和 Radio 6 Music。

> 本项目是独立的非官方开源项目，与 BBC 没有隶属、授权或背书关系。

## 功能

- 播放多个 BBC 广播直播频道。
- 首页显示当前节目、开始时间、预计结束时间和节目简介。
- 展示接下来的节目，时间自动转换为手机本地时区。
- 使用动态均衡器区分播放、暂停、连接和错误状态。
- 支持后台播放、锁屏控制、耳机按键和系统媒体通知。
- 可选择仅在播放期间保持屏幕常亮。
- 不申请定位权限，不需要 BBC 账号，不包含 VPN 或位置伪装功能。

## 技术实现

- Kotlin
- AndroidX Media3 / ExoPlayer
- `MediaSessionService`
- BBC Media Selector 与 RMS schedule 网络接口
- DASH / HLS 直播播放

播放器会优先动态解析当前可用的直播地址，并在解析失败时尝试备用地址。BBC 可以随时调整接口、节目权限和 CDN 地址，因此不保证所有频道在所有国家、网络或时间都可用。

## 构建

需要：

- JDK 17
- Android SDK Platform 36
- Android SDK Build-Tools 36.0.0

项目包含 Gradle Wrapper。在 Android Studio 中打开本目录，或在命令行运行：

```powershell
./gradlew.bat test lintDebug assembleDebug
```

Linux/macOS：

```bash
./gradlew test lintDebug assembleDebug
```

调试 APK 会生成在 `app/build/outputs/apk/debug/app-debug.apk`。

## 隐私

应用不包含分析 SDK，也不向项目维护者的服务器发送数据。播放和节目表功能会直接连接 BBC 及其 CDN；这些第三方服务会像普通网络请求一样接收到 IP 地址、User-Agent 等连接信息。详情见 [PRIVACY.md](PRIVACY.md)。

## 参与贡献

欢迎提交问题和 Pull Request。开始前请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。安全问题请按照 [SECURITY.md](SECURITY.md) 私下报告。

## 开源许可证与第三方内容

本项目代码以 [Apache License 2.0](LICENSE) 发布。

Apache License 2.0 只授权本仓库中由贡献者编写的源代码，不授予任何 BBC 节目、音频、节目元数据、名称、标志、商标或第三方服务的权利。BBC 及频道名称是相应权利人的财产。使用者和分发者需要自行遵守适用法律、BBC 条款、内容许可及应用商店政策。
