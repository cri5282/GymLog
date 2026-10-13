package it.gymlog.app.timer

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import it.gymlog.app.GymLogApp
import it.gymlog.app.MainActivity
import it.gymlog.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Mostra il conto alla rovescia in notifica e avvisa (notifica, suono, vibrazione) a fine recupero. */
class RestTimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val container = (application as GymLogApp).container
        val timer = container.restTimer
        ServiceCompat.startForeground(
            this, ONGOING_ID, ongoing(timer.state.value.remainingSeconds(System.currentTimeMillis())),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        if (job == null) job = scope.launch {
            timer.state.collectLatest { s ->
                val end = s.endAtMillis ?: run { stopSelfCleanly(); return@collectLatest }
                acquireWakeLock(end - System.currentTimeMillis() + 15_000)
                while (true) {
                    val left = s.remainingSeconds(System.currentTimeMillis())
                    if (left == 0) break
                    notify(ONGOING_ID, ongoing(left))
                    delay(1_000)
                }
                val settings = container.settings.current()
                notify(DONE_ID, done(settings.timerSound))
                if (settings.timerVibration) vibrate()
                stopSelfCleanly()
            }
        }
        return START_NOT_STICKY
    }

    private fun stopSelfCleanly() {
        releaseWakeLock()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
        job = null
    }

    override fun onDestroy() {
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }

    @SuppressLint("MissingPermission")
    private fun notify(id: Int, n: Notification) {
        val nm = NotificationManagerCompat.from(this)
        if (nm.areNotificationsEnabled()) nm.notify(id, n)
    }

    private fun contentIntent(): PendingIntent = PendingIntent.getActivity(
        this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun ongoing(seconds: Int): Notification =
        NotificationCompat.Builder(this, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("Recupero")
            .setContentText("%d:%02d".format(seconds / 60, seconds % 60))
            .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .setContentIntent(contentIntent())
            .build()

    private fun done(sound: Boolean): Notification =
        NotificationCompat.Builder(this, if (sound) CHANNEL_DONE_SOUND else CHANNEL_DONE_SILENT)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("Recupero finito")
            .setContentText("Pronto per la prossima serie")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true).setTimeoutAfter(60_000)
            .setContentIntent(contentIntent())
            .build()

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 400), -1))
    }

    private fun acquireWakeLock(timeoutMs: Long) {
        releaseWakeLock()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GymLog:rest").apply {
            acquire(timeoutMs.coerceIn(1_000, 20 * 60_000))
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    companion object {
        const val CHANNEL_ONGOING = "rest_ongoing"
        const val CHANNEL_DONE_SOUND = "rest_done_sound"
        const val CHANNEL_DONE_SILENT = "rest_done_silent"
        private const val ONGOING_ID = 1
        private const val DONE_ID = 2

        fun createChannels(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannels(listOf(
                NotificationChannel(CHANNEL_ONGOING, "Recupero in corso", NotificationManager.IMPORTANCE_LOW),
                NotificationChannel(CHANNEL_DONE_SOUND, "Fine recupero", NotificationManager.IMPORTANCE_HIGH).apply {
                    enableVibration(false)
                },
                NotificationChannel(CHANNEL_DONE_SILENT, "Fine recupero (silenzioso)", NotificationManager.IMPORTANCE_HIGH).apply {
                    enableVibration(false); setSound(null, null)
                },
            ))
        }

        fun start(context: Context) {
            runCatching { context.startForegroundService(Intent(context, RestTimerService::class.java)) }
        }
    }
}
