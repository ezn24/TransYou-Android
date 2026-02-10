![](./images/logo.svg)
# TransYou

[**English**](README.md)
| [**简体中文**](README.zh-CN.md)
| [**繁體中文**](README.zh-TW.md) 
<br>

**TransYou** is an Offline Android media transcoder app built with Kotlin + Jetpack Compose + Media3 Transformer.

## Warning
All code in this repository is written by Codex. So know your own risk!

## Features
- Video / audio transcoding UI with format and codec options.
- Audio-only export (remove video track).
- Silent-video export (remove audio track).
- Multiple output formats (e.g. mp4, mkv, webm, mov, mp3, wav, flac, ogg, opus, etc.).
- Output naming presets and custom template placeholders.
- In-app runtime logs preview + full logs dialog.
- Light / dark / pure-black theme and language options.

## Custom output filename usage
1. Select an input file and choose the target output format.
2. In **Output naming pattern**, choose **Custom template**.
3. Enter a template in **Custom name template** using placeholders.
4. (Optional) Set **Custom date format** for `{custom_date}` (for example: `yyyyMMdd_HHmmss`).
5. Confirm the **Output filename preview**, then start transcoding.

Common placeholders:
- `{input_file_name}`
- `{output_format}` / `{input_format}`
- `{input_encode}` / `{output_encode}`
- `{audio_encode}` / `{video_encode}`
- `{date}` / `{time}` / `{date_time}` / `{custom_date}`

Example template:
- `{input_file_name}_{output_encode}_{date_time}`

## Build (local)
> This repository currently uses Android Gradle Plugin `8.6.1` and Kotlin Android plugin `1.9.24`.

```bash
gradle :app:assembleDebug
```

If your environment cannot resolve AGP artifacts, make sure your Gradle repositories can access Google Maven.

## Project structure
- `app/src/main/java/com/github/ezn24/TransYou/` — app source code.
- `app/src/main/res/` — Android resources (strings, themes, icons, etc.).
- `app/build.gradle.kts` — Android module build config.

## License
This project is licensed under the GNU General Public License v3.0 (GPL-3.0). See [LICENSE](LICENSE).


