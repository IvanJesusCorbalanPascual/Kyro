package com.example.kyro

import android.app.Service
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import java.util.TreeMap
class MonitorService: Service() {

    // El handler ayuda a repetir una tarea periodicamente
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    // El intervalo en el que comprueba, en este caso cada 2 segundos
    private val CHECK_INTERVAL = 2000L

    override fun onCreate() {
        super.onCreate()
        Log.d("MonitorService", "Modo Focus Activado: ¡Listo para trabajar!")
        iniciarBucleDeVigilancia()
    }

    // Se ejecuta al llamar a startService
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Si el servicio muere intenta reactivarlo
        return START_STICKY
    }

    private fun iniciarBucleDeVigilancia() {
        runnable = object : Runnable {
            override fun run() {
                // Detecta que app esta usando en primer plano
                detectarAppEnPrimerPlano()

                // Vuelve a ejecutarlo en 2 segundos
                handler.postDelayed(this, CHECK_INTERVAL)
            }
        }
        // Esto arranca en la primera vuelta del bucle
        handler.post(runnable)
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
                    // La última en el mapa es la que esta en la pantalla
                    val currentApp = sortedMap.lastEntry()?.value?.packageName

                    // Lo muestra en el log para que poder verlo
                    Log.d("MonitorService", "App Actual: $currentApp")
                }

            }
        } catch (e: Exception) {
            Log.e("MonitorService", "Error al intentar detectar app", e)
        }
    }

    // Limpia al cerrarlo
    override fun onDestroy() {
        super.onDestroy()
        // Para el bucle de detección para que no consuma
        handler.removeCallbacks(runnable)
        Log.d("MonitorService", "Modo Focus desactivado.")
    }

    override fun onBind(intent: Intent?): IBinder? {
            return null
    }
}