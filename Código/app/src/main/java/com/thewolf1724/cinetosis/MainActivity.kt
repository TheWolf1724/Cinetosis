package com.thewolf1724.cinetosis

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.thewolf1724.cinetosis.data.Settings
import com.thewolf1724.cinetosis.data.SettingsRepository
import com.thewolf1724.cinetosis.service.OverlayService
import com.thewolf1724.cinetosis.ui.theme.CinetosisTheme
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CinetosisTheme {
                CinetosisScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CinetosisScreen() {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val settings by repository.settings.collectAsState(initial = Settings())

    // Estados que dependen del sistema; se refrescan al volver a la app.
    var canDrawOverlay by remember { mutableStateOf(AndroidSettings.canDrawOverlays(context)) }
    var serviceRunning by remember { mutableStateOf(OverlayService.isRunning) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canDrawOverlay = AndroidSettings.canDrawOverlays(context)
                serviceRunning = OverlayService.isRunning
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* el resultado no bloquea el flujo */ }

    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringRes(context, R.string.app_name)) }) },
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringRes(context, R.string.intro_text),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))

            if (!canDrawOverlay) {
                PermissionCard(
                    text = stringRes(context, R.string.permission_needed),
                    buttonText = stringRes(context, R.string.grant_permission),
                    onClick = {
                        val intent = Intent(
                            AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}"),
                        )
                        context.startActivity(intent)
                    },
                )
                Spacer(Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    if (serviceRunning) {
                        OverlayService.stop(context)
                        serviceRunning = false
                    } else {
                        ensureNotificationPermission()
                        OverlayService.start(context)
                        serviceRunning = true
                    }
                },
                enabled = canDrawOverlay,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringRes(
                        context,
                        if (serviceRunning) R.string.stop_overlay else R.string.start_overlay,
                    ),
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(
                stringRes(context, R.string.section_appearance),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))

            LabeledSlider(
                label = stringRes(context, R.string.sensitivity) + ": ${(settings.sensitivity * 100).roundToInt()}%",
                value = settings.sensitivity,
                range = 0f..1f,
                onChange = { v -> scope.launch { repository.update { it.copy(sensitivity = v) } } },
            )
            LabeledSlider(
                label = stringRes(context, R.string.dots_per_edge) + ": ${settings.dotsPerEdge}",
                value = settings.dotsPerEdge.toFloat(),
                range = 2f..12f,
                onChange = { v -> scope.launch { repository.update { it.copy(dotsPerEdge = v.roundToInt()) } } },
            )
            LabeledSlider(
                label = stringRes(context, R.string.dot_size) + ": ${settings.dotSizeDp.roundToInt()} dp",
                value = settings.dotSizeDp,
                range = 2f..12f,
                onChange = { v -> scope.launch { repository.update { it.copy(dotSizeDp = v) } } },
            )

            Spacer(Modifier.height(16.dp))
            Text(
                stringRes(context, R.string.section_edges),
                style = MaterialTheme.typography.titleMedium,
            )
            EdgeToggle(stringRes(context, R.string.edge_top), settings.edgeTop) { c ->
                scope.launch { repository.update { it.copy(edgeTop = c) } }
            }
            EdgeToggle(stringRes(context, R.string.edge_bottom), settings.edgeBottom) { c ->
                scope.launch { repository.update { it.copy(edgeBottom = c) } }
            }
            EdgeToggle(stringRes(context, R.string.edge_left), settings.edgeLeft) { c ->
                scope.launch { repository.update { it.copy(edgeLeft = c) } }
            }
            EdgeToggle(stringRes(context, R.string.edge_right), settings.edgeRight) { c ->
                scope.launch { repository.update { it.copy(edgeRight = c) } }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                stringRes(context, R.string.section_language),
                style = MaterialTheme.typography.titleMedium,
            )
            LanguageSelector()

            Spacer(Modifier.height(24.dp))
            Text(
                text = stringRes(context, R.string.disclaimer),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PermissionCard(text: String, buttonText: String, onClick: () -> Unit) {
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

/**
 * Selector de idioma. Aplica el cambio en caliente mediante AppCompat
 * ([AppCompatDelegate.setApplicationLocales]), que recrea la actividad con el nuevo idioma.
 */
@Composable
private fun LanguageSelector() {
    val context = LocalContext.current
    // Etiqueta de idioma actual ("" = predeterminado del sistema).
    val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        .substringBefore('-')

    val options = listOf(
        "" to stringRes(context, R.string.language_system),
        "es" to stringRes(context, R.string.language_spanish),
        "en" to stringRes(context, R.string.language_english),
    )

    Column(Modifier.fillMaxWidth()) {
        options.forEach { (tag, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
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

/** Pequeño helper para leer strings sin necesitar un Context composable extra. */
private fun stringRes(context: android.content.Context, resId: Int): String = context.getString(resId)
