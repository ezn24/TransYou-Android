# TransYou

TransYou is an Android media transcoder app built with Kotlin + Jetpack Compose + Media3 Transformer.

## Languages
- English (this file)
- 简体中文: [README.zh-CN.md](README.zh-CN.md)
- 繁體中文: [README.zh-TW.md](README.zh-TW.md)

## Features
- Video / audio transcoding UI with format and codec options.
- Audio-only export (remove video track).
- Silent-video export (remove audio track).
- Multiple output formats (e.g. mp4, mkv, webm, mov, mp3, wav, flac, ogg, opus, etc.).
- Output naming presets and custom template placeholders.
- In-app runtime logs preview + full logs dialog.
- Light / dark / pure-black theme and language options.

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
No license file is currently defined in this repository.
