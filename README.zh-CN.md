# TransYou

TransYou 是一款 Android 媒体转码应用，基于 Kotlin + Jetpack Compose + Media3 Transformer 构建。

## 语言
- English: [README.md](README.md)
- 简体中文（当前）
- 繁體中文: [README.zh-TW.md](README.zh-TW.md)

## 功能
- 视频 / 音频转码界面，支持多种格式与编码配置。
- 仅输出音频（移除视频轨）。
- 输出无声视频（移除音频轨）。
- 支持多种输出格式（如 mp4、mkv、webm、mov、mp3、wav、flac、ogg、opus 等）。
- 支持输出文件名推荐模板与自定义占位符模板。
- 应用内可查看运行日志预览与完整日志弹窗。
- 支持浅色 / 深色 / 纯黑主题与语言切换。

## 本地构建
> 当前仓库使用 Android Gradle Plugin `8.6.1` 与 Kotlin Android plugin `1.9.24`。

```bash
gradle :app:assembleDebug
```

若构建环境无法解析 AGP 依赖，请确认 Gradle 仓库可访问 Google Maven。

## 目录结构
- `app/src/main/java/com/github/ezn24/TransYou/`：应用源码。
- `app/src/main/res/`：Android 资源（字符串、主题、图标等）。
- `app/build.gradle.kts`：Android 模块构建配置。

## 许可证
当前仓库尚未提供 License 文件。
