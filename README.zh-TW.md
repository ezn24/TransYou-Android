![](./images/logo.svg)

# TransYou

[**English**](README.md)
| [**简体中文**](README.zh-CN.md)
| [**繁體中文**](README.zh-TW.md) 
<br>

**TransYou** 是一款 Android 離綫媒體轉碼應用，基於 Kotlin + Jetpack Compose + Media3 Transformer 建置。

## 注意！！！
本倉庫中的所有程式碼皆由 Codex 編寫。請知曉此風險。

## 功能
- 提供影片 / 音訊轉碼介面，支援多種格式與編碼設定。
- 僅輸出音訊（移除影片軌）。
- 輸出無聲影片（移除音訊軌）。
- 支援多種輸出格式（如 mp4、mkv、webm、mov、mp3、wav、flac、ogg、opus 等）。
- 支援輸出檔名推薦模板與自訂佔位符模板。
- App 內可查看執行日誌預覽與完整日誌視窗。
- 支援淺色 / 深色 / 純黑主題與語言切換。

## 自訂輸出檔名使用說明
1. 先選擇輸入檔案，並設定目標輸出格式。
2. 在 **輸出命名方式** 中選擇 **自訂模板**。
3. 在 **自訂命名模板** 中輸入佔位符模板。
4. （可選）在 **自訂日期格式** 中設定 `{custom_date}`（例如：`yyyyMMdd_HHmmss`）。
5. 確認 **輸出檔名預覽** 無誤後開始轉碼。

常用佔位符：
- `{input_file_name}`
- `{output_format}` / `{input_format}`
- `{input_encode}` / `{output_encode}`
- `{audio_encode}` / `{video_encode}`
- `{date}` / `{time}` / `{date_time}` / `{custom_date}`

範例模板：
- `{input_file_name}_{output_encode}_{date_time}`

## 本機建置
> 目前倉庫使用 Android Gradle Plugin `8.6.1` 與 Kotlin Android plugin `1.9.24`。

```bash
gradle :app:assembleDebug
```

若建置環境無法解析 AGP 相依套件，請確認 Gradle 倉庫可存取 Google Maven。

## 專案結構
- `app/src/main/java/com/github/ezn24/TransYou/`：應用程式原始碼。
- `app/src/main/res/`：Android 資源（字串、主題、圖示等）。
- `app/build.gradle.kts`：Android 模組建置設定。

## 授權
本專案採用 GNU General Public License v3.0（GPL-3.0）授權，詳見 [LICENSE](LICENSE)。
