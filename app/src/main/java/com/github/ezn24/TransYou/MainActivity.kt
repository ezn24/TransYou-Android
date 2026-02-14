package com.github.ezn24.TransYou

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.TransformationRequest
import androidx.media3.transformer.Transformer
import com.github.ezn24.TransYou.ui.theme.TransYouTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.dataStore by preferencesDataStore(name = "settings")

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsRepository = SettingsRepository(this)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.languageFlow
                    .distinctUntilChanged()
                    .collect { language ->
                        val targetLocales = language.toLocaleList()
                        if (AppCompatDelegate.getApplicationLocales() != targetLocales) {
                            AppCompatDelegate.setApplicationLocales(targetLocales)
                        }
                    }
            }
        }

        setContent {
            val themeMode by settingsRepository.themeModeFlow.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val pureBlack by settingsRepository.pureBlackFlow.collectAsStateWithLifecycle(false)

            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            TransYouTheme(darkTheme = darkTheme, pureBlack = pureBlack) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppScaffold(settingsRepository = settingsRepository)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppScaffold(settingsRepository: SettingsRepository) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (currentRoute == Routes.Settings.route) {
                            stringResource(id = R.string.title_settings)
                        } else {
                            stringResource(id = R.string.title_transcode)
                        },
                    )
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == Routes.Transcode.route,
                    onClick = { navController.navigate(Routes.Transcode.route) },
                    icon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                    label = { Text(stringResource(id = R.string.nav_transcode)) },
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.Settings.route,
                    onClick = { navController.navigate(Routes.Settings.route) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(id = R.string.nav_settings)) },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Transcode.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            composable(Routes.Transcode.route) {
                TranscodeScreen(snackbarHostState = snackbarHostState)
            }
            composable(Routes.Settings.route) {
                SettingsScreen(settingsRepository = settingsRepository)
            }
        }
    }
}

private enum class Routes(val route: String) {
    Transcode("transcode"),
    Settings("settings"),
}

@Composable
private fun TranscodeScreen(
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val logEntries = remember { mutableStateListOf<String>() }
    var isTranscoding by rememberSaveable { mutableStateOf(false) }

    var inputFile by rememberSaveable { mutableStateOf("/storage/emulated/0/Movies/input.mov") }
    var outputFolder by rememberSaveable { mutableStateOf("/storage/emulated/0/Movies") }
    var outputNamePattern by rememberSaveable { mutableStateOf(OutputNamePattern.DATE_ONLY) }
    var customNameTemplate by rememberSaveable { mutableStateOf("{input_file_name}.convert.{input_format}.to.{output_format}") }
    var customDateFormat by rememberSaveable { mutableStateOf("yyyyMMdd") }
    var videoCodec by rememberSaveable { mutableStateOf("libx264") }
    var audioCodec by rememberSaveable { mutableStateOf("aac") }
    var outputFormat by rememberSaveable { mutableStateOf("mp4") }
    var resolution by rememberSaveable { mutableStateOf("1920x1080") }
    var bitrate by rememberSaveable { mutableStateOf("4000") }
    var preset by rememberSaveable { mutableStateOf("medium") }
    var audioOnly by rememberSaveable { mutableStateOf(false) }
    var muteVideo by rememberSaveable { mutableStateOf(false) }
    val inputFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                inputFile = uri.toString()
            }
        },
    )
    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                inputFile = uri.toString()
            }
        },
    )
    val outputFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            if (uri != null) {
                outputFolder = uri.toString()
            }
        },
    )

    val inputFileName = remember(inputFile) { extractInputFileName(inputFile) }
    val inputFormat = remember(inputFile) { extractInputFormat(inputFile) }
    val outputEncode = remember(videoCodec, audioCodec, audioOnly, muteVideo) {
        when {
            audioOnly -> audioCodec
            muteVideo -> videoCodec
            else -> "${videoCodec}_${audioCodec}"
        }
    }
    val generatedOutputName = remember(
        outputNamePattern,
        customNameTemplate,
        customDateFormat,
        inputFileName,
        inputFormat,
        outputFormat,
        audioCodec,
        videoCodec,
        outputEncode,
    ) {
        generateOutputName(
            pattern = outputNamePattern,
            customTemplate = customNameTemplate,
            datePattern = customDateFormat,
            inputFileName = inputFileName,
            inputFormat = inputFormat,
            inputEncode = "unknown",
            outputFormat = outputFormat,
            outputEncode = outputEncode,
            audioEncode = audioCodec,
            videoEncode = videoCodec,
        )
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionCard(title = stringResource(id = R.string.section_inputs)) {
                OutlinedTextField(
                    value = inputFile,
                    onValueChange = { inputFile = it },
                    label = { Text(stringResource(id = R.string.input_file)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FilledTonalButton(
                        onClick = {
                            mediaPicker.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageAndVideo,
                                ),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = stringResource(id = R.string.pick_from_gallery))
                    }
                    FilledTonalButton(
                        onClick = {
                            inputFilePicker.launch(arrayOf("video/*", "audio/*"))
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = stringResource(id = R.string.pick_from_files))
                    }
                }
            }

        SectionCard(title = stringResource(id = R.string.section_output)) {
            OutlinedTextField(
                value = outputFolder,
                onValueChange = { outputFolder = it },
                label = { Text(stringResource(id = R.string.output_folder)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            FilledTonalButton(onClick = { outputFolderPicker.launch(null) }) {
                Text(text = stringResource(id = R.string.pick_output_folder))
            }
            Spacer(modifier = Modifier.height(12.dp))
            DropdownField(
                label = stringResource(id = R.string.output_name_pattern),
                options = OutputNamePattern.entries,
                selected = outputNamePattern,
                onSelected = { outputNamePattern = it },
                optionLabel = { mode -> stringResource(id = mode.labelRes) },
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (outputNamePattern == OutputNamePattern.CUSTOM_TEMPLATE) {
                OutlinedTextField(
                    value = customNameTemplate,
                    onValueChange = { customNameTemplate = it },
                    label = { Text(stringResource(id = R.string.custom_name_template)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = customDateFormat,
                    onValueChange = { customDateFormat = it },
                    label = { Text(stringResource(id = R.string.custom_date_format)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.custom_name_help),
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            OutlinedTextField(
                value = "$generatedOutputName.$outputFormat",
                onValueChange = {},
                label = { Text(stringResource(id = R.string.output_name_preview)) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
            )
        }

        SectionCard(title = stringResource(id = R.string.section_video)) {
            DropdownField(
                label = stringResource(id = R.string.video_codec),
                options = listOf(
                    "copy",
                    "libx264",
                    "libx265",
                    "libvpx-vp9",
                    "libvpx",
                    "mpeg4",
                    "av1",
                    "hevc",
                    "vp9",
                ),
                selected = videoCodec,
                onSelected = { videoCodec = it },
                enabled = !audioOnly,
            )
            Spacer(modifier = Modifier.height(12.dp))
            DropdownField(
                label = stringResource(id = R.string.resolution),
                options = listOf(
                    "copy",
                    "3840x2160",
                    "2560x1440",
                    "1920x1080",
                    "1600x900",
                    "1280x720",
                    "1024x576",
                    "854x480",
                    "640x360",
                    "480x270",
                ),
                selected = resolution,
                onSelected = { resolution = it },
                enabled = !audioOnly && videoCodec != "copy",
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = bitrate,
                onValueChange = { bitrate = it },
                label = { Text(stringResource(id = R.string.bitrate)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                enabled = !audioOnly && videoCodec != "copy",
            )
            Spacer(modifier = Modifier.height(12.dp))
            DropdownField(
                label = stringResource(id = R.string.preset),
                options = listOf("ultrafast", "fast", "medium", "slow", "veryslow"),
                selected = preset,
                onSelected = { preset = it },
                enabled = !audioOnly && videoCodec != "copy",
            )
        }

        SectionCard(title = stringResource(id = R.string.section_audio)) {
            DropdownField(
                label = stringResource(id = R.string.audio_codec),
                options = listOf(
                    "copy",
                    "aac",
                    "libopus",
                    "libmp3lame",
                    "flac",
                    "vorbis",
                    "ac3",
                    "eac3",
                ),
                selected = audioCodec,
                onSelected = { audioCodec = it },
                enabled = !muteVideo,
            )
        }

        SectionCard(title = stringResource(id = R.string.section_format)) {
            DropdownField(
                label = stringResource(id = R.string.output_format),
                options = listOf(
                    "mp4", "mkv", "webm", "mov", "m4v", "3gp", "avi", "ts",
                    "mp3", "wav", "m4a", "aac", "flac", "ogg", "opus",
                ),
                selected = outputFormat,
                onSelected = { outputFormat = it },
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(id = R.string.audio_only_output))
                Switch(
                    checked = audioOnly,
                    onCheckedChange = { enabled ->
                        audioOnly = enabled
                        if (enabled) muteVideo = false
                    },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(id = R.string.mute_video_output))
                Switch(
                    checked = muteVideo,
                    onCheckedChange = { enabled ->
                        muteVideo = enabled
                        if (enabled) audioOnly = false
                    },
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = if (logEntries.isEmpty()) {
                        stringResource(id = R.string.log_empty)
                    } else {
                        logEntries.takeLast(8).joinToString(separator = "\n")
                    },
                    onValueChange = {},
                    label = { Text(text = stringResource(id = R.string.log_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    minLines = 6,
                )
            }
        }

            if (isTranscoding) {
                SectionCard(title = stringResource(id = R.string.transcoding_progress)) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(id = R.string.transcoding_running))
                }
            }
        }

        FloatingActionButton(
            onClick = {
                if (isTranscoding) return@FloatingActionButton
                val message = context.getString(R.string.transcoding_running)
                val inputUri = resolveInputUri(inputFile)
                val outputPath = resolveOutputFile(context, outputFolder, generatedOutputName, outputFormat)
                val shouldRemoveVideo = audioOnly
                val shouldRemoveAudio = muteVideo
                isTranscoding = true
                logEntries.clear()
                logEntries.add("[init] ${context.getString(R.string.transcoding_progress)}")
                logEntries.add("[mode] audioOnly=$audioOnly, muteVideo=$muteVideo, format=$outputFormat")

                val transformationRequest = TransformationRequest.Builder()
                    .setVideoMimeType(videoMimeTypeFor(videoCodec, outputFormat, shouldRemoveVideo))
                    .setAudioMimeType(audioMimeTypeFor(audioCodec, outputFormat, shouldRemoveAudio))
                    .build()

                val editedMediaItem = EditedMediaItem.Builder(MediaItem.fromUri(inputUri))
                    .setRemoveVideo(shouldRemoveVideo)
                    .setRemoveAudio(shouldRemoveAudio)
                    .build()


                val transformer = Transformer.Builder(context)
                    .setTransformationRequest(transformationRequest)
                    .addListener(
                        object : Transformer.Listener {
                            override fun onCompleted(
                                composition: androidx.media3.transformer.Composition,
                                exportResult: ExportResult,
                            ) {
                                coroutineScope.launch {
                                    logEntries.add("[done] Transcode finished")
                                    logEntries.add("[file] ${outputPath.absolutePath}")
                                    if (outputFolder.startsWith("content://")) {
                                        copyToOutputFolder(context, outputFolder, outputPath)
                                    }
                                    isTranscoding = false
                                }
                            }

                            override fun onError(
                                composition: androidx.media3.transformer.Composition,
                                exportResult: ExportResult,
                                exportException: ExportException,
                            ) {
                                coroutineScope.launch {
                                    logEntries.add("[error] ${exportException.message ?: "Unknown"}")
                                    isTranscoding = false
                                }
                            }
                        },
                    )
                    .build()

                transformer.start(editedMediaItem, outputPath.absolutePath)
                logEntries.add("[run] ${outputPath.absolutePath}")
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .navigationBarsPadding(),
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
        }
    }


}

@Composable
private fun SettingsScreen(settingsRepository: SettingsRepository) {
    val themeMode by settingsRepository.themeModeFlow.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
    val pureBlack by settingsRepository.pureBlackFlow.collectAsStateWithLifecycle(false)
    val language by settingsRepository.languageFlow.collectAsStateWithLifecycle(AppLanguage.SYSTEM)
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionCard(title = stringResource(id = R.string.language)) {
            DropdownField(
                label = stringResource(id = R.string.language),
                options = AppLanguage.entries.map { it.labelRes },
                selected = language.labelRes,
                onSelected = { labelRes ->
                    val selected = AppLanguage.entries.first { it.labelRes == labelRes }
                    coroutineScope.launch { settingsRepository.setLanguage(selected) }
                },
                optionLabel = { resId -> stringResource(id = resId) },
            )
        }

        SectionCard(title = stringResource(id = R.string.theme)) {
            DropdownField(
                label = stringResource(id = R.string.theme),
                options = ThemeMode.entries.map { it.labelRes },
                selected = themeMode.labelRes,
                onSelected = { labelRes ->
                    val selected = ThemeMode.entries.first { it.labelRes == labelRes }
                    coroutineScope.launch { settingsRepository.setThemeMode(selected) }
                },
                optionLabel = { resId -> stringResource(id = resId) },
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = stringResource(id = R.string.pure_black))
                Switch(
                    checked = pureBlack,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch { settingsRepository.setPureBlack(enabled) }
                    },
                )
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownField(
    label: String,
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    enabled: Boolean = true,
    optionLabel: @Composable (T) -> String = { it.toString() },
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
    ) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(text = label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            enabled = enabled,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (enabled) options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}


private enum class OutputNamePattern(val labelRes: Int, val template: String) {
    DATE_ONLY(R.string.output_name_date_only, "{date}"),
    CONVERT_DETAIL(R.string.output_name_convert_detail, "{input_file_name}.convert.{input_format}.to.{output_format}"),
    ORIGINAL_NAME(R.string.output_name_original, "{input_file_name}"),
    NAME_WITH_DATE_TIME(R.string.output_name_with_datetime, "{input_file_name}_{date_time}"),
    NAME_WITH_OUTPUT_ENCODE(R.string.output_name_with_encode, "{input_file_name}_{output_encode}"),
    DATE_TIME_ONLY(R.string.output_name_datetime_only, "{date_time}"),
    CUSTOM_TEMPLATE(R.string.output_name_custom, "{input_file_name}"),
}

private enum class ThemeMode(val labelRes: Int) {
    SYSTEM(R.string.theme_system),
    LIGHT(R.string.theme_light),
    DARK(R.string.theme_dark),
}

private enum class AppLanguage(val labelRes: Int, val localeTag: String?) {
    SYSTEM(R.string.language_system, null),
    EN(R.string.language_en, "en"),
    ZH_CN(R.string.language_zh_cn, "zh-CN"),
    ZH_TW(R.string.language_zh_tw, "zh-TW");

    fun toLocaleList(): LocaleListCompat = if (localeTag == null) {
        LocaleListCompat.getEmptyLocaleList()
    } else {
        LocaleListCompat.forLanguageTags(localeTag)
    }
}

private class SettingsRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val pureBlackKey = booleanPreferencesKey("pure_black")
    private val languageKey = stringPreferencesKey("language")

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val value = prefs[themeKey]
        ThemeMode.entries.firstOrNull { it.name == value } ?: ThemeMode.SYSTEM
    }

    val pureBlackFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[pureBlackKey] ?: false
    }

    val languageFlow: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        val value = prefs[languageKey]
        AppLanguage.entries.firstOrNull { it.name == value } ?: AppLanguage.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        updatePreference(themeKey, mode.name)
    }

    suspend fun setPureBlack(enabled: Boolean) {
        updatePreference(pureBlackKey, enabled)
    }

    suspend fun setLanguage(language: AppLanguage) {
        updatePreference(languageKey, language.name)
    }

    private suspend fun <T> updatePreference(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { prefs ->
            prefs[key] = value
        }
    }
}

private fun extractInputFileName(input: String): String {
    val candidate = if (input.startsWith("content://")) {
        Uri.parse(input).lastPathSegment ?: "input"
    } else {
        File(input).name
    }
    return candidate.substringBeforeLast('.', candidate).ifBlank { "input" }
}

private fun extractInputFormat(input: String): String {
    val candidate = if (input.startsWith("content://")) {
        Uri.parse(input).lastPathSegment ?: ""
    } else {
        File(input).name
    }
    return candidate.substringAfterLast('.', "unknown").ifBlank { "unknown" }
}

private fun generateOutputName(
    pattern: OutputNamePattern,
    customTemplate: String,
    datePattern: String,
    inputFileName: String,
    inputFormat: String,
    inputEncode: String,
    outputFormat: String,
    outputEncode: String,
    audioEncode: String,
    videoEncode: String,
): String {
    val safeDatePattern = datePattern.ifBlank { "yyyyMMdd" }
    val dateFormatter = runCatching { SimpleDateFormat(safeDatePattern, Locale.getDefault()) }
        .getOrElse { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }
    val date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
    val dateTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val customDate = dateFormatter.format(Date())

    val template = if (pattern == OutputNamePattern.CUSTOM_TEMPLATE) customTemplate else pattern.template
    val resolved = template
        .replace("{input_file_name}", inputFileName)
        .replace("{output_format}", outputFormat)
        .replace("{input_format}", inputFormat)
        .replace("{input_encode}", inputEncode)
        .replace("{output_encode}", outputEncode)
        .replace("{audio_encode}", audioEncode)
        .replace("{video_encode}", videoEncode)
        .replace("{date_time}", dateTime)
        .replace("{date}", date)
        .replace("{custom_date}", customDate)
        .replace("{time}", SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date()))

    return resolved
        .replace('/', '_')
        .replace('\\', '_')
        .replace(':', '-')
        .trim('.')
        .ifBlank { "output_$dateTime" }
}

private fun resolveInputUri(input: String): Uri {
    return if (input.startsWith("content://")) Uri.parse(input) else Uri.fromFile(File(input))
}

private fun resolveOutputFile(
    context: Context,
    outputFolder: String,
    outputName: String,
    outputFormat: String,
): File {
    val safeName = outputName.ifBlank { "output" }
    return if (outputFolder.startsWith("content://")) {
        File(context.cacheDir, "$safeName.$outputFormat")
    } else {
        File(outputFolder.trimEnd('/'), "$safeName.$outputFormat")
    }
}

private fun copyToOutputFolder(context: Context, folderUri: String, source: File) {
    val treeUri = Uri.parse(folderUri)
    val documentFile = DocumentFile.fromTreeUri(context, treeUri) ?: return
    // Keep original filename/extension as-is (e.g., .ogg) instead of provider-normalized variants (e.g., .oga).
    val target = documentFile.createFile("application/octet-stream", source.name) ?: return
    context.contentResolver.openOutputStream(target.uri)?.use { outputStream ->
        source.inputStream().use { inputStream ->
            inputStream.copyTo(outputStream)
        }
    }
}

private fun videoMimeTypeFor(videoCodec: String, outputFormat: String, removeVideo: Boolean): String? {
    if (removeVideo || videoCodec == "copy") return null
    return when {
        outputFormat in setOf("mp4", "mov", "m4v", "3gp") -> MimeTypes.VIDEO_H264
        outputFormat == "webm" -> MimeTypes.VIDEO_VP9
        outputFormat == "ts" -> MimeTypes.VIDEO_H264
        outputFormat == "avi" -> MimeTypes.VIDEO_H264
        else -> null
    }
}

private fun audioMimeTypeFor(audioCodec: String, outputFormat: String, removeAudio: Boolean): String? {
    if (removeAudio || audioCodec == "copy") return null
    return when {
        outputFormat in setOf("mp4", "mov", "mkv", "m4a", "aac") -> MimeTypes.AUDIO_AAC
        outputFormat in setOf("webm", "ogg", "opus") -> MimeTypes.AUDIO_OPUS
        outputFormat == "mp3" -> MimeTypes.AUDIO_MPEG
        outputFormat == "wav" -> MimeTypes.AUDIO_RAW
        outputFormat == "flac" -> MimeTypes.AUDIO_FLAC
        else -> null
    }
}
