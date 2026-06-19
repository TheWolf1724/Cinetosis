package com.thewolf1724.cinetosis.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.thewolf1724.cinetosis.R
import kotlinx.coroutines.launch

private enum class PageKind { INTRO, OVERLAY, NOTIFICATIONS, TILE, FINISH }

/**
 * Tour de bienvenida en primera ejecución: explicación breve, una página por permiso (con botón
 * directo para concederlo) y una página final de bienvenida.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var canDrawOverlay by remember { mutableStateOf(Permissions.hasOverlay(context)) }
    var notifGranted by remember { mutableStateOf(Permissions.hasNotifications(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canDrawOverlay = Permissions.hasOverlay(context)
                notifGranted = Permissions.hasNotifications(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notifGranted = granted }

    val pages = listOf(
        PageKind.INTRO,
        PageKind.OVERLAY,
        PageKind.NOTIFICATIONS,
        PageKind.TILE,
        PageKind.FINISH,
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val isLast = pagerState.currentPage == pages.lastIndex

    fun goNext() {
        if (isLast) {
            onFinish()
        } else {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
        }
    }

    Scaffold { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        ) {
            // Botón "Saltar" arriba a la derecha (excepto en la última página)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                if (!isLast) {
                    TextButton(onClick = onFinish) { Text(stringResource(R.string.onb_skip)) }
                } else {
                    Spacer(Modifier.height(48.dp))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { index ->
                PageContent(
                    kind = pages[index],
                    canDrawOverlay = canDrawOverlay,
                    notifGranted = notifGranted,
                    onGrantOverlay = {
                        context.startActivity(Permissions.overlaySettingsIntent(context))
                    },
                    onAllowNotifications = {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    onAddTile = { Permissions.requestAddTile(context) },
                )
            }

            // Indicadores de página
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(pages.size) { i ->
                    val selected = i == pagerState.currentPage
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (selected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                            ),
                    )
                }
            }

            // Barra inferior: Atrás / Siguiente o Empezar
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }) { Text(stringResource(R.string.onb_back)) }
                } else {
                    Spacer(Modifier.size(1.dp))
                }
                Button(onClick = { goNext() }) {
                    Text(stringResource(if (isLast) R.string.onb_finish else R.string.onb_next))
                }
            }
        }
    }
}

@Composable
private fun PageContent(
    kind: PageKind,
    canDrawOverlay: Boolean,
    notifGranted: Boolean,
    onGrantOverlay: () -> Unit,
    onAllowNotifications: () -> Unit,
    onAddTile: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Ilustración
        when (kind) {
            PageKind.INTRO, PageKind.FINISH -> Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(R.string.content_desc_logo),
                modifier = Modifier.size(140.dp),
            )
            else -> PageIcon(
                when (kind) {
                    PageKind.OVERLAY -> Icons.Outlined.Layers
                    PageKind.NOTIFICATIONS -> Icons.Outlined.Notifications
                    else -> Icons.Outlined.Dashboard
                },
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(titleOf(kind)),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(bodyOf(kind)),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        // Acción específica de cada página (botón directo del permiso)
        when (kind) {
            PageKind.OVERLAY -> {
                if (canDrawOverlay) {
                    GrantedLabel()
                } else {
                    FilledTonalButton(onClick = onGrantOverlay) {
                        Text(stringResource(R.string.grant_permission))
                    }
                }
            }
            PageKind.NOTIFICATIONS -> {
                when {
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ->
                        Text(stringResource(R.string.onb_not_needed), color = MaterialTheme.colorScheme.primary)
                    notifGranted -> GrantedLabel()
                    else -> FilledTonalButton(onClick = onAllowNotifications) {
                        Text(stringResource(R.string.onb_notif_button))
                    }
                }
            }
            PageKind.TILE -> {
                if (Permissions.canRequestAddTile) {
                    FilledTonalButton(onClick = onAddTile) {
                        Text(stringResource(R.string.onb_tile_button))
                    }
                } else {
                    Text(
                        stringResource(R.string.onb_tile_manual),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun PageIcon(icon: ImageVector) {
    Box(
        Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(60.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun GrantedLabel() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.size(8.dp))
        Text(stringResource(R.string.onb_granted), color = MaterialTheme.colorScheme.primary)
    }
}

private fun titleOf(kind: PageKind): Int = when (kind) {
    PageKind.INTRO -> R.string.onb_intro_title
    PageKind.OVERLAY -> R.string.onb_overlay_title
    PageKind.NOTIFICATIONS -> R.string.onb_notif_title
    PageKind.TILE -> R.string.onb_tile_title
    PageKind.FINISH -> R.string.onb_finish_title
}

private fun bodyOf(kind: PageKind): Int = when (kind) {
    PageKind.INTRO -> R.string.onb_intro_body
    PageKind.OVERLAY -> R.string.onb_overlay_body
    PageKind.NOTIFICATIONS -> R.string.onb_notif_body
    PageKind.TILE -> R.string.onb_tile_body
    PageKind.FINISH -> R.string.onb_finish_body
}
