package com.thewolf1724.cinetosis.service

import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.TileService
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.thewolf1724.cinetosis.CinetosisApp
import com.thewolf1724.cinetosis.MainActivity
import com.thewolf1724.cinetosis.R
import com.thewolf1724.cinetosis.data.SettingsRepository
import com.thewolf1724.cinetosis.motion.MotionEngine
import com.thewolf1724.cinetosis.overlay.DotsView
import com.thewolf1724.cinetosis.tile.CinetosisTileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Servicio en primer plano que dibuja el overlay de puntos sobre cualquier app y alimenta la
 * vista con el vector del [MotionEngine].
 */
class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var motionEngine: MotionEngine
    private lateinit var repository: SettingsRepository
    private var dotsView: DotsView? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        motionEngine = MotionEngine(this)
        repository = SettingsRepository(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundCompat()
        addOverlay()
        observeSettings()
        motionEngine.start()
        isRunning = true
        refreshTile()
        return START_STICKY
    }

    /** Pide al sistema refrescar el Quick Settings Tile para que refleje el estado real. */
    private fun refreshTile() {
        TileService.requestListeningState(
            this,
            ComponentName(this, CinetosisTileService::class.java),
        )
    }

    private fun addOverlay() {
        if (dotsView != null) return
        val view = DotsView(this) { motionEngine.motionX to motionEngine.motionY }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )
        windowManager.addView(view, params)
        // Ajusta los puntos a la zona visible (fuera de la barra de estado/navegación).
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setInsets(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(view)
        dotsView = view
    }

    private fun observeSettings() {
        repository.settings.onEach { s ->
            motionEngine.sensitivity = s.sensitivity
            val density = resources.displayMetrics.density
            val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            dotsView?.apply {
                maxShiftPx = (MIN_SHIFT_DP + s.amplitude * (MAX_SHIFT_DP - MIN_SHIFT_DP)) * density
                applyColors(this, s.adaptiveColor, s.colorArgb, night)
                dotRadiusPx = s.dotSizeDp * density
                dotsPerEdge = s.dotsPerEdge
                edgeTop = s.edgeTop
                edgeBottom = s.edgeBottom
                edgeLeft = s.edgeLeft
                edgeRight = s.edgeRight
            }
        }.launchIn(scope)
    }

    private fun applyColors(view: DotsView, adaptive: Boolean, colorArgb: Int, night: Boolean) {
        if (adaptive) {
            // Color base según el tema del sistema; el halo da contraste sobre cualquier fondo.
            view.dotColor = if (night) 0xF2FFFFFF.toInt() else 0xF21A1A1A.toInt()
            view.haloColor = if (night) 0x80000000.toInt() else 0x80FFFFFF.toInt()
        } else {
            view.dotColor = colorArgb
            view.haloColor = if (ColorUtils.calculateLuminance(colorArgb) > 0.5) {
                0x80000000.toInt()
            } else {
                0x80FFFFFF.toInt()
            }
        }
    }

    private fun startForegroundCompat() {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CinetosisApp.CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setSmallIcon(R.drawable.ic_tile)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        motionEngine.stop()
        dotsView?.let { windowManager.removeView(it) }
        dotsView = null
        scope.cancel()
        isRunning = false
        refreshTile()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.thewolf1724.cinetosis.action.START"
        const val ACTION_STOP = "com.thewolf1724.cinetosis.action.STOP"
        private const val NOTIFICATION_ID = 1001

        // Rango del recorrido de los puntos (en dp) mapeado desde el ajuste de amplitud (0..1).
        const val MIN_SHIFT_DP = 16f
        const val MAX_SHIFT_DP = 70f

        /** Estado observable por la UI y el Tile. */
        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java).setAction(ACTION_START)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, OverlayService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
