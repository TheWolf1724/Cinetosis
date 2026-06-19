package com.thewolf1724.cinetosis.service

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.thewolf1724.cinetosis.CinetosisApp
import com.thewolf1724.cinetosis.MainActivity
import com.thewolf1724.cinetosis.R
import com.thewolf1724.cinetosis.data.DetectionMode
import com.thewolf1724.cinetosis.data.SettingsRepository
import com.thewolf1724.cinetosis.detection.VehicleClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.sqrt

/**
 * Servicio en primer plano (silencioso) que detecta de forma offline si el usuario va en coche y,
 * en ese caso, **enciende el overlay automáticamente**. El apagado es manual (el usuario).
 *
 * Estados: IDLE (esperando, sensores casi apagados) → CLASSIFYING (muestreo corto) → ACTIVE
 * (overlay encendido, sin sensores) → COOLDOWN (tras apagado manual, espera antes de re-armar).
 *
 * Consumo: en IDLE usa `TYPE_SIGNIFICANT_MOTION` (disparador por hardware, ~0 batería). Si el
 * sistema entra en ahorro de batería, se fuerza el modo BATTERY. GPS solo en modos no-batería.
 */
class DetectionService : Service(), SensorEventListener {

    private enum class State { IDLE, CLASSIFYING, ACTIVE, COOLDOWN }

    private lateinit var sensorManager: SensorManager
    private lateinit var powerManager: PowerManager
    private var locationManager: LocationManager? = null
    private lateinit var repository: SettingsRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())

    private val sigMotionSensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION)
    }
    private val linearAccel: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    }

    @Volatile private var autoDetect = true
    @Volatile private var mode = DetectionMode.BATTERY
    private var fromBoot = false

    private var state = State.IDLE
    private val window = ArrayList<Float>(128)
    private var maxSpeed = 0f
    private var gpsActive = false
    private var gpsUsedThisWindow = false

    private val sigMotionListener = object : TriggerEventListener() {
        override fun onTrigger(event: TriggerEvent?) {
            // El disparador se auto-desactiva tras dispararse; pasamos a clasificar.
            if (state == State.IDLE) startClassifying()
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (location.hasSpeed()) maxSpeed = maxOf(maxSpeed, location.speed)
        }

        // Métodos abstractos en Android < 30 (deprecados después): se implementan vacíos.
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        repository = SettingsRepository(applicationContext)
        repository.settings.onEach { s ->
            autoDetect = s.autoDetect
            mode = s.detectionMode
            if (!s.autoDetect) stopSelf()
        }.launchIn(scope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        fromBoot = intent?.getBooleanExtra(EXTRA_FROM_BOOT, false) ?: false
        try {
            startForegroundCompat()
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }
        isRunning = true
        if (state == State.IDLE) armIdle()
        return START_STICKY
    }

    private fun effectiveMode(): DetectionMode =
        if (powerManager.isPowerSaveMode) DetectionMode.BATTERY else mode

    private fun useLocation(): Boolean =
        !fromBoot &&
            effectiveMode() != DetectionMode.BATTERY &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun armIdle() {
        state = State.IDLE
        window.clear()
        maxSpeed = 0f
        val sig = sigMotionSensor
        if (sig != null) {
            sensorManager.requestTriggerSensor(sigMotionListener, sig)
        } else {
            // Sin sensor de movimiento significativo: clasificación continua (más batería).
            startClassifying()
        }
    }

    private fun startClassifying() {
        state = State.CLASSIFYING
        window.clear()
        maxSpeed = 0f
        gpsUsedThisWindow = false
        linearAccel?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        if (useLocation()) startGps()
        handler.postDelayed(::finishClassifying, classifyWindowMs())
    }

    private fun classifyWindowMs(): Long =
        if (effectiveMode() == DetectionMode.EXTREME) 6_000L else 8_000L

    private fun finishClassifying() {
        sensorManager.unregisterListener(this)
        stopGps()
        val mags = window.toFloatArray()
        val durationSec = classifyWindowMs() / 1000f
        val byMotion = VehicleClassifier.isVehicleByMotion(mags, durationSec)
        val bySpeed = gpsUsedThisWindow && VehicleClassifier.isVehicleBySpeed(maxSpeed)
        val vehicle = when (effectiveMode()) {
            DetectionMode.BATTERY -> byMotion
            else -> byMotion || bySpeed
        }
        val canShow = android.provider.Settings.canDrawOverlays(this)
        when {
            vehicle && autoDetect && canShow && !OverlayService.isRunning -> {
                OverlayService.start(this)
                enterActive()
            }
            vehicle && OverlayService.isRunning -> enterActive()
            else -> armIdle()
        }
    }

    private fun enterActive() {
        state = State.ACTIVE
        handler.postDelayed(::pollActive, ACTIVE_POLL_MS)
    }

    private fun pollActive() {
        if (state != State.ACTIVE) return
        if (!OverlayService.isRunning) {
            // El overlay se apagó (manual): esperamos antes de poder re-activar.
            state = State.COOLDOWN
            handler.postDelayed(::endCooldown, COOLDOWN_MS)
        } else {
            handler.postDelayed(::pollActive, ACTIVE_POLL_MS)
        }
    }

    private fun endCooldown() {
        if (state == State.COOLDOWN) armIdle()
    }

    private fun startGps() {
        val lm = locationManager ?: return
        try {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    GPS_MIN_TIME_MS,
                    0f,
                    locationListener,
                    Looper.getMainLooper(),
                )
                gpsActive = true
                gpsUsedThisWindow = true
            }
        } catch (e: SecurityException) {
            gpsActive = false
        }
    }

    private fun stopGps() {
        if (gpsActive) {
            locationManager?.removeUpdates(locationListener)
            gpsActive = false
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
            .setContentTitle(getString(R.string.detect_notif_title))
            .setContentText(getString(R.string.detect_notif_text))
            .setSmallIcon(R.drawable.ic_tile)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
            .build()

        val loc = useLocation()
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                if (loc) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                startForeground(NOTIFICATION_ID, notification, type)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && loc ->
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            else -> startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (state == State.CLASSIFYING && event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            window.add(sqrt(x * x + y * y + z * z))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* no-op */ }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        sigMotionSensor?.let { sensorManager.cancelTriggerSensor(sigMotionListener, it) }
        sensorManager.unregisterListener(this)
        stopGps()
        scope.cancel()
        isRunning = false
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.thewolf1724.cinetosis.action.STOP_DETECTION"
        private const val EXTRA_FROM_BOOT = "from_boot"
        private const val NOTIFICATION_ID = 2001

        private const val ACTIVE_POLL_MS = 5_000L
        private const val COOLDOWN_MS = 120_000L  // 2 min tras apagado manual
        private const val GPS_MIN_TIME_MS = 1_000L

        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context, fromBoot: Boolean = false) {
            val intent = Intent(context, DetectionService::class.java)
                .putExtra(EXTRA_FROM_BOOT, fromBoot)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, DetectionService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
