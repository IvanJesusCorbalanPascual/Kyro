package com.example.kyro

import android.app.Service
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import java.util.TreeMap
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat

class MonitorService: Service() {

    // Ayuda a repetir una tarea periodicamente con corrutines, Dispatchers.IO esta optimizado para esto
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    // Para controlar el estado del bucle
    private var isMonitoring = false

    // Detecta la última app y la recuerda para no repetir y consumir en exceso
    private var lastDetectedPackage = ""

    // Lista de aplicaciones a ignorar
    private val ignoredPackages = setOf(
        "com.android.systemui",
        "com.google.android.apps.nexuslauncher",
        "com.google.android.permissioncontroller",
        // Ignora nuestra app
        "com.example.kyro"
    )

    // El intervalo en el que comprueba, en este caso cada 2 segundos
    private val CHECK_INTERVAL = 2000L

    override fun onCreate() {
        super.onCreate()
        Log.d("MonitorService", "Modo Focus Activado: Listo para trabajar")
    }

    // Se ejecuta al llamar a startService
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isMonitoring) {
            isMonitoring = true
            iniciarBucleDeVigilancia()
        }
        return START_STICKY
    }

    private fun iniciarBucleDeVigilancia() {
        // Lanza la corrutina en segundo plano
        serviceScope.launch {
            while (isMonitoring) {
                detectarAppEnPrimerPlano()
                // Suspende la corrutina sin bloquear el hilo para ahorrar batería
                delay(CHECK_INTERVAL)
            }
        }
    }

    private fun detectarAppEnPrimerPlano() {
        try {

            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val time = System.currentTimeMillis()

            // Pide las estadisticas en los últimos 10 segundos
            val appList = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                time - 1000 * 10,
                time
            )

            if (appList != null && appList.isNotEmpty()) {
                // Ordena por última vez usada para saber cual es la más reciente o actual
                val sortedMap = TreeMap<Long, UsageStats>()
                for (usageStats in appList) {
                    sortedMap[usageStats.lastTimeUsed] = usageStats
                }
                if (sortedMap.isNotEmpty()) {
                    // La última en el mapa es la que esta en la pantalla, si es null sale de la función
                    val currentApp = sortedMap.lastEntry()?.value?.packageName ?: return
                    // Solo actua si detecta que la app es diferente a la anterior y no esta ignorada
                    if (currentApp != lastDetectedPackage && currentApp !in ignoredPackages) {

                        lastDetectedPackage = currentApp
                        Log.d("MonitorService", "Cambio de contexto de app: $currentApp")

                        // Aquí ira la lógica de detección de distracciones
                        if (esAppDeDistraccion(currentApp)) {
                            Log.w("MonitorService", "App de distracción detectada en: $currentApp")

                            mostrarNotificacionDistraccion()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MonitorService", "Error al intentar detectar app", e)
        }
    }

    // Función para mantener el código limpio
    private fun esAppDeDistraccion(packageName: String): Boolean {
        // Lista de apps detectadas como distracción, en el futuro podra configurarlo el usuario
        val blackList = listOf(
            "com.google.android.youtube",
            "com.instagram.android",
            "com.zhiliaoapp.musically"
        )
        return packageName in blackList
    }

    // Limpia al cerrarlo
    override fun onDestroy() {
        super.onDestroy()
        // Cancela las corrutinas y evita posibles fugas de memoria
        serviceScope.cancel()
        Log.d("MonitorService", "Modo Focus desactivado.")
    }

    override fun onBind(intent: Intent?): IBinder? {
            return null
    }

    // Función que lanza notificaciones desde el Servicio
    private fun mostrarNotificacionDistraccion() {
        val channelId = "kyro_focus_channel"
        val notificationId = 1001

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crea el canal de notificaciones
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas de Modo Focus",
                // Importancia alta, el telefono vibra
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para volver a estudiar"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Gestiona lo que pasa al tocar la notificación
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        // Construye la notificacion
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_kyro)
            // Fuerza el icono grande a color
            .setContentTitle("Un paso más")
            .setContentText("Cada minuto cuenta. Si vuelves ahora, tu yo del futuro te lo agradecerá \uD83D\uDE42")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            // La notificación desaparece al tocarla
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}