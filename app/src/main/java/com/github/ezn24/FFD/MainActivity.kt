package com.github.ezn24.FFD

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.ezn24.FFD.ui.theme.FFDTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsRepository = remember { SettingsRepository(this) }
            val themeMode by settingsRepository.themeModeFlow.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val pureBlack by settingsRepository.pureBlackFlow.collectAsStateWithLifecycle(false)
            val language by settingsRepository.languageFlow.collectAsStateWithLifecycle(AppLanguage.SYSTEM)

            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            LaunchedEffect(language) {
                AppCompatDelegate.setApplicationLocales(language.toLocaleList())
            }

            FFDTheme(darkTheme = darkTheme, pureBlack = pureBlack) {
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
    val snackbarHostState = remember { SnackbarHostState() }
    var isTranscoding by rememberSaveable { mutableStateOf(false) }
    var progress by rememberSaveable { mutableStateOf(0f) }
    val logEntries = remember { mutableStateOf(listOf<String>()) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (navController.currentDestinationRoute() == Routes.Settings.route) {
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
                    selected = navController.currentDestinationRoute() == Routes.Transcode.route,
                    onClick = { navController.navigate(Routes.Transcode.route) },
                    icon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                    label = { Text(stringResource(id = R.string.nav_transcode)) },
                )
                NavigationBarItem(
                    selected = navController.currentDestinationRoute() == Routes.Settings.route,
                    onClick = { navController.navigate(Routes.Settings.route) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(id = R.string.nav_settings)) },
                )
            }
        },
        floatingActionButton = {
            if (navController.currentDestinationRoute() == Routes.Transcode.route) {
                FloatingActionButton(
                    onClick = {
                        if (!isTranscoding) {
                            isTranscoding = true
                            progress = 0f
                            logEntries.value = listOf("[init] Preparing ffmpeg session")
                            coroutineScope.launch {
                                repeat(10) { step ->
                                    progress = (step + 1) / 10f
                                    logEntries.value = logEntries.value +
                                        "[progress] ${((step + 1) * 10)}%"
                                    kotlinx.coroutines.delay(350)
                                }
                                logEntries.value = logEntries.value + "[done] Transcode finished"
                                isTranscoding = false
                            }
                        }
                    },
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                }
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
                TranscodeScreen(
                    snackbarHostState = snackbarHostState,
                    isTranscoding = isTranscoding,
                    progress = progress,
                    logEntries = logEntries.value,
                )
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

private fun NavHostController.currentDestinationRoute(): String? =
    currentBackStackEntry?.destination?.route

@Composable
private fun TranscodeScreen(
    snackbarHostState: SnackbarHostState,
    isTranscoding: Boolean,
    progress: Float,
    logEntries: List<String>,
) {
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var inputFile by rememberSaveable { mutableStateOf("/storage/emulated/0/Movies/input.mov") }
    var outputFolder by rememberSaveable { mutableStateOf("/storage/emulated/0/Movies") }
    var outputName by rememberSaveable { mutableStateOf("output") }
    var videoCodec by rememberSaveable { mutableStateOf("libx264") }
    var audioCodec by rememberSaveable { mutableStateOf("aac") }
    var outputFormat by rememberSaveable { mutableStateOf("mp4") }
    var resolution by rememberSaveable { mutableStateOf("1920x1080") }
    var bitrate by rememberSaveable { mutableStateOf("4000") }
    var preset by rememberSaveable { mutableStateOf("medium") }
    var extraOptions by rememberSaveable { mutableStateOf("-movflags +faststart") }
    var showLogs by rememberSaveable { mutableStateOf(false) }

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

    val generatedCommand = remember(
        inputFile,
        outputFolder,
        outputName,
        videoCodec,
        audioCodec,
        outputFormat,
        resolution,
        bitrate,
        preset,
        extraOptions,
    ) {
        buildFfmpegCommand(
            inputFile = inputFile,
            outputFolder = outputFolder,
            outputName = outputName,
            videoCodec = videoCodec,
            audioCodec = audioCodec,
            outputFormat = outputFormat,
            resolution = resolution,
            bitrate = bitrate,
            preset = preset,
            extraOptions = extraOptions,
        )
    }

    var commandText by rememberSaveable { mutableStateOf(generatedCommand) }
    var isManualEdit by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(generatedCommand, isManualEdit) {
        if (!isManualEdit) {
            commandText = generatedCommand
        }
    }

    val scrollState = rememberScrollState()

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
            OutlinedTextField(
                value = outputName,
                onValueChange = { outputName = it },
                label = { Text(stringResource(id = R.string.output_name)) },
                modifier = Modifier.fillMaxWidth(),
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
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = bitrate,
                onValueChange = { bitrate = it },
                label = { Text(stringResource(id = R.string.bitrate)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            DropdownField(
                label = stringResource(id = R.string.preset),
                options = listOf("ultrafast", "fast", "medium", "slow", "veryslow"),
                selected = preset,
                onSelected = { preset = it },
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
            )
        }

        SectionCard(title = stringResource(id = R.string.section_format)) {
            DropdownField(
                label = stringResource(id = R.string.output_format),
                options = listOf("mp4", "mkv", "webm", "mov", "mp3", "wav"),
                selected = outputFormat,
                onSelected = { outputFormat = it },
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = extraOptions,
                onValueChange = { extraOptions = it },
                label = { Text(stringResource(id = R.string.extra_options)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionCard(title = stringResource(id = R.string.section_command)) {
            OutlinedTextField(
                value = commandText,
                onValueChange = {
                    commandText = it
                    isManualEdit = true
                },
                label = { Text(stringResource(id = R.string.command_preview)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.35f),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FilledTonalButton(onClick = {
                    clipboardManager.setText(AnnotatedString(commandText))
                    keyboardController?.hide()
                    val message = context.getString(R.string.snackbar_copied)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = message,
                        )
                    }
                }) {
                    Text(text = stringResource(id = R.string.copy_command))
                }
                TextButton(onClick = {
                    isManualEdit = false
                    commandText = generatedCommand
                    keyboardController?.hide()
                    val message = context.getString(R.string.snackbar_reset)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = message,
                        )
                    }
                }) {
                    Text(text = stringResource(id = R.string.reset_command))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = { showLogs = true }) {
                Text(text = stringResource(id = R.string.view_logs))
            }
        }

        if (isTranscoding) {
            SectionCard(title = stringResource(id = R.string.transcoding_progress)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = stringResource(id = R.string.transcoding_running))
            }
        }
    }

    if (showLogs) {
        Dialog(onDismissRequest = { showLogs = false }) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.log_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (logEntries.isEmpty()) {
                        Text(text = stringResource(id = R.string.log_empty))
                    } else {
                        logEntries.forEach { entry ->
                            Text(text = entry)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    FilledTonalButton(onClick = { showLogs = false }) {
                        Text(text = stringResource(id = R.string.close))
                    }
                }
            }
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
    optionLabel: @Composable (T) -> String = { it.toString() },
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
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
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
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

private fun buildFfmpegCommand(
    inputFile: String,
    outputFolder: String,
    outputName: String,
    videoCodec: String,
    audioCodec: String,
    outputFormat: String,
    resolution: String,
    bitrate: String,
    preset: String,
    extraOptions: String,
): String {
    val sanitizedInput = inputFile.ifBlank { "input" }
    val safeName = outputName.ifBlank { "output" }
    val sanitizedOutput = "${outputFolder.trimEnd('/')}/$safeName.$outputFormat"
    val options = listOfNotNull(
        "-i \"$sanitizedInput\"",
        if (videoCodec == "copy") "-c:v copy" else if (videoCodec.isNotBlank()) "-c:v $videoCodec" else null,
        if (resolution.isNotBlank() && resolution != "copy") "-s $resolution" else null,
        if (bitrate.isNotBlank() && videoCodec != "copy") "-b:v ${bitrate}k" else null,
        if (preset.isNotBlank() && videoCodec != "copy") "-preset $preset" else null,
        if (audioCodec == "copy") "-c:a copy" else if (audioCodec.isNotBlank()) "-c:a $audioCodec" else null,
        if (extraOptions.isNotBlank()) extraOptions else null,
        "-f $outputFormat",
        "\"$sanitizedOutput\"",
    )

    return options.joinToString(" ").trim()
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
