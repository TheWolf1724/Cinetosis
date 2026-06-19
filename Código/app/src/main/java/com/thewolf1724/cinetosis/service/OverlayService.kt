package com.thewolf1724.cinetosis.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.thewolf1724.cinetosis.CinetosisApp
import com.thewolf1724.cinetosis.MainActivity
import com.thewolf1724.cinetosis.R
import com.thewolf1724.cinetosis.data.SettingsRepository
import com.thewolf1724.cinetosis.motion.MotionEngine
import com.thewolf1724.cinetosis.overlay.DotsView
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
        return START_STICKY
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
        dotsView = view
    }

    private fun observeSettings() {
        repository.settings.onEach { s ->
            motionEngine.sensitivity = s.sensitivity
            dotsView?.apply {
                dotColor = s.colorArgb
                dotRadiusPx = s.dotSizeDp * resources.displayMetrics.density
                dotsPerEdge = s.dotsPerEdge
                edgeTop = s.edgeTop
                edgeBottom = s.edgeBottom
                edgeLeft = s.edgeLeft
                edgeRight = s.edgeRight
            }
        }.launchIn(scope)
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
            .setPriority(NotificationCompat.PRIORITY_LOW)
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
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.thewolf1724.cinetosis.action.START"
        const val ACTION_STOP = "com.thewolf1724.cinetosis.action.STOP"
        private const val NOTIFICATION_ID = 1001

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
