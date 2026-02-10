# TransYou
English: [README.md](README.md) 繁體中文: [README.zh-TW.md](README.zh-TW.md)

<br>

**TransYou** 是一款 Android 本地媒体转码应用，基于 Kotlin + Jetpack Compose + Media3 Transformer 构建。

## 注意！！！
本仓库中的所有代码均由 Codex 编写。请认知此风险。

## 功能
- 视频 / 音频转码界面，支持多种格式与编码配置。
- 仅输出音频（移除视频轨）。
- 输出无声视频（移除音频轨）。
- 支持多种输出格式（如 mp4、mkv、webm、mov、mp3、wav、flac、ogg、opus 等）。
- 支持输出文件名推荐模板与自定义占位符模板。
- 应用内可查看运行日志预览与完整日志弹窗。
- 支持浅色 / 深色 / 纯黑主题与语言切换。

## 自定义输出文件名使用说明
1. 先选择输入文件，并设置目标输出格式。
2. 在 **输出命名方式** 中选择 **自定义模板**。
3. 在 **自定义命名模板** 中输入占位符模板。
4. （可选）在 **自定义日期格式** 中设置 `{custom_date}`（例如：`yyyyMMdd_HHmmss`）。
5. 确认 **输出文件名预览** 无误后开始转码。

常用占位符：
- `{input_file_name}`
- `{output_format}` / `{input_format}`
- `{input_encode}` / `{output_encode}`
- `{audio_encode}` / `{video_encode}`
- `{date}` / `{time}` / `{date_time}` / `{custom_date}`

示例模板：
- `{input_file_name}_{output_encode}_{date_time}`

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
本项目采用 GNU General Public License v3.0（GPL-3.0）协议，详见 [LICENSE](LICENSE)。

