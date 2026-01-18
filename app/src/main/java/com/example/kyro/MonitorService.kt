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
import android.os.Build
import androidx.core.app.NotificationCompat
import io.github.jan.supabase.postgrest.postgrest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class MonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var isMonitoring = false
    private var lastDetectedPackage = ""
    private val sentNotifications = mutableSetOf<String>()

    private val ignoredPackages = setOf(
        "com.android.systemui",
        "com.google.android.apps.nexuslauncher",
        "com.google.android.permissioncontroller",
        "com.example.kyro"
    )

    private val CHECK_INTERVAL = 2000L

    override fun onCreate() {
        super.onCreate()
        Log.d("MonitorService", "Servicio de monitorización creado.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isMonitoring) {
            isMonitoring = true
            iniciarBucleDeVigilancia()
        }
        return START_STICKY
    }

    private fun iniciarBucleDeVigilancia() {
        serviceScope.launch {
            while (isMonitoring) {
                detectarAppEnPrimerPlano()
                checkCompletedExams()
                checkTaskNotifications()
                checkExamNotifications()
                delay(CHECK_INTERVAL)
            }
        }
    }

    private fun checkCompletedExams() {
        serviceScope.launch {
            try {
                val uncompletedExams = SupabaseClient.client.postgrest["examenes"]
                    .select { filter { eq("completada", false) } }
                    .decodeList<Examen>()

                val now = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

                for (exam in uncompletedExams) {
                    if (exam.hora_examen != null) {
                        try {
                            val examDateTime = LocalDateTime.parse("${exam.fecha_examen} ${exam.hora_examen}", formatter)
                            if (now.isAfter(examDateTime)) {
                                SupabaseClient.client.postgrest["examenes"].update(
                                    { set("completada", true) },
                                    { filter { eq("id", exam.id!!) } }
                                )
                            }
                        } catch (e: DateTimeParseException) {
                            Log.e("MonitorService", "Error al parsear fecha del examen: ${exam.fecha_examen}", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MonitorService", "Error al comprobar los exámenes completados", e)
            }
        }
    }

    private fun checkTaskNotifications() {
        serviceScope.launch {
            try {
                val upcomingTasks = SupabaseClient.client.postgrest["tareas"]
                    .select { filter { eq("completada", false) } }
                    .decodeList<Tarea>()

                for (task in upcomingTasks) {
                    checkAndSendNotification(task, task.notificacion1, "notif1")
                    checkAndSendNotification(task, task.notificacion2, "notif2")
                }
            } catch (e: Exception) {
                Log.e("MonitorService", "Error al comprobar notificaciones de tareas", e)
            }
        }
    }

    private fun checkExamNotifications() {
        serviceScope.launch {
            try {
                val upcomingExams = SupabaseClient.client.postgrest["examenes"]
                    .select { filter { eq("completada", false) } }
                    .decodeList<Examen>()

                for (exam in upcomingExams) {
                    checkAndSendNotification(exam, exam.notificacion1, "notif1")
                    checkAndSendNotification(exam, exam.notificacion2, "notif2")
                }
            } catch (e: Exception) {
                Log.e("MonitorService", "Error al comprobar notificaciones de exámenes", e)
            }
        }
    }

    private fun checkAndSendNotification(event: Any, notificationTime: String?, notifType: String) {
        if (notificationTime == null || notificationTime == "No notificar") return

        val eventId: Long
        val eventDateTime: LocalDateTime
        val eventType: String

        when (event) {
            is Tarea -> {
                if (event.hora_entrega == null) return
                eventId = event.id!!
                eventDateTime = LocalDateTime.parse("${event.fecha_entrega} ${event.hora_entrega}", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                eventType = "task"
            }
            is Examen -> {
                if (event.hora_examen == null) return
                eventId = event.id!!
                eventDateTime = LocalDateTime.parse("${event.fecha_examen} ${event.hora_examen}", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                eventType = "exam"
            }
            else -> return
        }

        val notificationId = "$eventType-$eventId-$notifType"
        if (notificationId in sentNotifications) return

        val notificationDateTime = getNotificationTime(eventDateTime, notificationTime)

        if (LocalDateTime.now().isAfter(notificationDateTime)) {
            when (event) {
                is Tarea -> sendTaskNotification(event)
                is Examen -> sendExamNotification(event)
            }
            sentNotifications.add(notificationId)
        }
    }

    private fun getNotificationTime(taskDateTime: LocalDateTime, notificationTime: String): LocalDateTime {
        return when (notificationTime) {
            "En el momento del evento" -> taskDateTime
            "5 minutos antes" -> taskDateTime.minusMinutes(5)
            "10 minutos antes" -> taskDateTime.minusMinutes(10)
            "30 minutos antes" -> taskDateTime.minusMinutes(30)
            "1 hora antes" -> taskDateTime.minusHours(1)
            "1 día antes" -> taskDateTime.minusDays(1)
            else -> taskDateTime
        }
    }

    private fun sendTaskNotification(task: Tarea) {
        val intent = Intent(this, EventDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("id", task.id)
            putExtra("type", "tarea")
            putExtra("title", task.nombre_tarea)
            putExtra("description", task.descripcion)
            putExtra("date", task.fecha_entrega)
            putExtra("hora_entrega", task.hora_entrega)
            putExtra("notif1", task.notificacion1)
            putExtra("notif2", task.notificacion2)
            putExtra("completada", task.completada)
        }
        val pendingIntent = PendingIntent.getActivity(this, task.id!!.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        sendNotification("Recordatorio de Tarea: ${task.nombre_tarea}", task.descripcion, pendingIntent, task.id.toInt())
    }

    private fun sendExamNotification(exam: Examen) {
        val intent = Intent(this, EventDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("id", exam.id)
            putExtra("type", "examen")
            putExtra("title", exam.nombre_examen)
            putExtra("description", exam.descripcion)
            putExtra("date", exam.fecha_examen)
            putExtra("hora_examen", exam.hora_examen)
            putExtra("notif1", exam.notificacion1)
            putExtra("notif2", exam.notificacion2)
            putExtra("completada", exam.completada)
        }
        val pendingIntent = PendingIntent.getActivity(this, exam.id!!.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        sendNotification("Recordatorio de Examen: ${exam.nombre_examen}", exam.descripcion, pendingIntent, exam.id.toInt())
    }

    private fun sendNotification(title: String, content: String, pendingIntent: PendingIntent, notificationId: Int) {
        val channelId = "kyro_reminders_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Recordatorios", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Recordatorios de tareas y exámenes"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_kyro)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }


    private fun detectarAppEnPrimerPlano() {
        val sharedPref = getSharedPreferences("KyroPrefs", Context.MODE_PRIVATE)
        val isFocusEnabled = sharedPref.getBoolean("FOCUS_ENABLED", true)

        if (!isFocusEnabled) return

        try {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val time = System.currentTimeMillis()
            val appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time)

            if (appList.isNotEmpty()) {
                val sortedMap = TreeMap<Long, UsageStats>()
                for (usageStats in appList) {
                    sortedMap[usageStats.lastTimeUsed] = usageStats
                }
                val currentApp = sortedMap.lastEntry()?.value?.packageName ?: return
                if (currentApp != lastDetectedPackage && currentApp !in ignoredPackages) {
                    lastDetectedPackage = currentApp
                    if (esAppDeDistraccion(currentApp)) {
                        mostrarNotificacionDistraccion()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MonitorService", "Error al detectar app", e)
        }
    }

    private fun esAppDeDistraccion(packageName: String): Boolean {
        val blackList = listOf(
            "com.google.android.youtube", "com.instagram.android", "com.zhiliaoapp.musically",
            "com.netflix.mediaclient", "tv.twitch.android.app", "com.hbo.hbonow",
            "com.amazon.avod.thirdpartyclient", "com.discord", "com.facebook.katana",
            "com.twitter.android", "com.pinterest", "com.reddit.frontpage"
        )
        return packageName in blackList
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d("MonitorService", "Servicio de monitorización destruido.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun mostrarNotificacionDistraccion() {
        val channelId = "kyro_focus_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Alertas de Modo Focus", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notificaciones para volver a estudiar"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_kyro)
            .setContentTitle("¡Ey! Un pequeño recordatorio")
            .setContentText("Cada minuto de estudio cuenta. ¡Vuelve a la tarea y acércate a tus metas!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
