package com.thewolf1724.cinetosis.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.thewolf1724.cinetosis.R
import com.thewolf1724.cinetosis.data.DetectionMode
import com.thewolf1724.cinetosis.data.Settings
import com.thewolf1724.cinetosis.data.SettingsRepository
import com.thewolf1724.cinetosis.service.DetectionService
import com.thewolf1724.cinetosis.service.OverlayService
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val settings by repository.settings.collectAsState(initial = Settings())

    var canDrawOverlay by remember { mutableStateOf(Permissions.hasOverlay(context)) }
    var serviceRunning by remember { mutableStateOf(OverlayService.isRunning) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canDrawOverlay = Permissions.hasOverlay(context)
                serviceRunning = OverlayService.isRunning
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* el resultado no bloquea el flujo */ }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* si se deniega, los modos GPS caen a detección por sensores */ }

    // Mantiene el servicio de detección en marcha cuando procede (auto-encendido invisible).
    LaunchedEffect(settings.autoDetect, settings.detectionMode, canDrawOverlay) {
        if (settings.autoDetect && canDrawOverlay) {
            DetectionService.start(context, fromBoot = false)
        } else if (DetectionService.isRunning) {
            DetectionService.stop(context)
        }
    }

    fun toggleOverlay() {
        if (serviceRunning) {
            OverlayService.stop(context)
            serviceRunning = false
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Permissions.hasNotifications(context)) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            OverlayService.start(context)
            serviceRunning = true
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            StatusCard(
                running = serviceRunning,
                enabled = canDrawOverlay,
                onToggle = { toggleOverlay() },
            )

            Spacer(Modifier.height(16.dp))

            if (!canDrawOverlay) {
                ActionCard(
                    text = stringResource(R.string.permission_needed),
                    buttonText = stringResource(R.string.grant_permission),
                    onClick = { context.startActivity(Permissions.overlaySettingsIntent(context)) },
                )
                Spacer(Modifier.height(16.dp))
            }

            // Consejo + botón para añadir el acceso rápido (tile)
            TileCard()

            Spacer(Modifier.height(24.dp))
            DetectionSection(
                settings = settings,
                onAutoDetect = { v -> scope.launch { repository.update { it.copy(autoDetect = v) } } },
                onMode = { m ->
                    scope.launch { repository.update { it.copy(detectionMode = m) } }
                    if (m != DetectionMode.BATTERY && !Permissions.hasLocation(context)) {
                        locationPermission.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                onBoot = { v -> scope.launch { repository.update { it.copy(autoStartOnBoot = v) } } },
            )

            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.section_appearance), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LabeledSlider(
                label = stringResource(R.string.sensitivity) + ": ${(settings.sensitivity * 100).roundToInt()}%",
                value = settings.sensitivity,
                range = 0f..1f,
                onChange = { v -> scope.launch { repository.update { it.copy(sensitivity = v) } } },
            )
            LabeledSlider(
                label = stringResource(R.string.amplitude) + ": ${(settings.amplitude * 100).roundToInt()}%",
                value = settings.amplitude,
                range = 0f..1f,
                onChange = { v -> scope.launch { repository.update { it.copy(amplitude = v) } } },
            )
            LabeledSlider(
                label = stringResource(R.string.dots_per_edge) + ": ${settings.dotsPerEdge}",
                value = settings.dotsPerEdge.toFloat(),
                range = 2f..12f,
                onChange = { v -> scope.launch { repository.update { it.copy(dotsPerEdge = v.roundToInt()) } } },
            )
            LabeledSlider(
                label = stringResource(R.string.dot_size) + ": ${settings.dotSizeDp.roundToInt()} dp",
                value = settings.dotSizeDp,
                range = 2f..12f,
                onChange = { v -> scope.launch { repository.update { it.copy(dotSizeDp = v) } } },
            )
            EdgeToggle(stringResource(R.string.adaptive_color), settings.adaptiveColor) { c ->
                scope.launch { repository.update { it.copy(adaptiveColor = c) } }
            }

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.section_test), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.demo_hint), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            MotionPreview(settings)

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.section_edges), style = MaterialTheme.typography.titleMedium)
            EdgeToggle(stringResource(R.string.edge_top), settings.edgeTop) { c ->
                scope.launch { repository.update { it.copy(edgeTop = c) } }
            }
            EdgeToggle(stringResource(R.string.edge_bottom), settings.edgeBottom) { c ->
                scope.launch { repository.update { it.copy(edgeBottom = c) } }
            }
            EdgeToggle(stringResource(R.string.edge_left), settings.edgeLeft) { c ->
                scope.launch { repository.update { it.copy(edgeLeft = c) } }
            }
            EdgeToggle(stringResource(R.string.edge_right), settings.edgeRight) { c ->
                scope.launch { repository.update { it.copy(edgeRight = c) } }
            }

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.section_language), style = MaterialTheme.typography.titleMedium)
            LanguageSelector()

            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.disclaimer),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatusCard(running: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (running) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(if (running) R.string.main_status_on else R.string.main_status_off),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.intro_text),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onToggle,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(if (running) R.string.stop_overlay else R.string.start_overlay))
            }
        }
    }
}

@Composable
private fun TileCard() {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Dashboard,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.size(12.dp))
            Column(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.main_tile_hint), style = MaterialTheme.typography.bodyMedium)
                if (Permissions.canRequestAddTile) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { Permissions.requestAddTile(context) }) {
                        Text(stringResource(R.string.main_add_tile))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionCard(text: String, buttonText: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Text(buttonText)
            }
        }
    }
}

@Composable
private fun DetectionSection(
    settings: Settings,
    onAutoDetect: (Boolean) -> Unit,
    onMode: (DetectionMode) -> Unit,
    onBoot: (Boolean) -> Unit,
) {
    Text(stringResource(R.string.section_detection), style = MaterialTheme.typography.titleMedium)
    EdgeToggle(stringResource(R.string.detect_auto), settings.autoDetect, onAutoDetect)

    if (settings.autoDetect) {
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.detect_mode), style = MaterialTheme.typography.bodyLarge)
        ModeOption(stringResource(R.string.mode_battery), settings.detectionMode == DetectionMode.BATTERY) {
            onMode(DetectionMode.BATTERY)
        }
        ModeOption(stringResource(R.string.mode_balanced), settings.detectionMode == DetectionMode.BALANCED) {
            onMode(DetectionMode.BALANCED)
        }
        ModeOption(stringResource(R.string.mode_extreme), settings.detectionMode == DetectionMode.EXTREME) {
            onMode(DetectionMode.EXTREME)
        }
        EdgeToggle(stringResource(R.string.detect_boot), settings.autoStartOnBoot, onBoot)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.detect_off_note), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.detect_powersave_note), style = MaterialTheme.typography.bodySmall)
        if (settings.detectionMode != DetectionMode.BATTERY) {
            Text(stringResource(R.string.detect_location_note), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ModeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun EdgeToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun LanguageSelector() {
    val current = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')
    val options = listOf(
        "" to stringResource(R.string.language_system),
        "es" to stringResource(R.string.language_spanish),
        "en" to stringResource(R.string.language_english),
    )
    Column(Modifier.fillMaxWidth()) {
        options.forEach { (tag, label) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = current == tag,
                    onClick = {
                        val locales = if (tag.isEmpty()) {
                            LocaleListCompat.getEmptyLocaleList()
                        } else {
                            LocaleListCompat.forLanguageTags(tag)
                        }
                        AppCompatDelegate.setApplicationLocales(locales)
                    },
                )
                Text(label, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
