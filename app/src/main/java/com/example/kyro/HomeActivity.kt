package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import android.app.AppOpsManager
import android.content.Context
import android.provider.Settings
import android.os.Process
import androidx.appcompat.app.AlertDialog

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Configura los botones de rapido acceso de la pantalla principal
        setupQuickActions()
    }

    // Se ejecuta siempre que el usuario ve esta pantalla
    override fun onResume() {
        super.onResume()
        // Llama al Helper y dile que ilumine "nav_home"
        NavigationHelper.setupBottomNavigation(this, R.id.nav_home)

        // Comprueba y pide los  en un dialogo para el modo de estudio Focus si no los tiene
        verificarYPedirPermisosFocus()
    }

    // Configuracion de los botones de las tarjetas
    private fun setupQuickActions() {
        val btnQuickAI = findViewById<MaterialCardView>(R.id.btnQuickAI)
        btnQuickAI.setOnClickListener {
            Toast.makeText(this, "Abriendo Kyro IA...", Toast.LENGTH_SHORT).show()
        }

        // Botón para ir a temario
        val btnQuickSyllabus = findViewById<MaterialCardView>(R.id.btnQuickSyllabus)
        btnQuickSyllabus.setOnClickListener {
            startActivity(Intent(this, TemarioActivity::class.java))
        }
    }

    // Logica del modo Focus, pregunta al sistema si tiene permisos para ver el historial de uso de apps
    private fun comprobarPermisoDeUso(): Boolean {
        // Obtiene el gestor de operaciones de apps del sistema
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

        // Comprueba el estado del permiso de uso de apps
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            // ID del proceso de Kyro
            Process.myUid(),
            // Nombre del paquete
            packageName
        )
        // Devuelve true si esta permitido, si no devuelve false
        return mode == AppOpsManager.MODE_ALLOWED
    }


    // Muestra el diálogo para pedir los permisos y reedirige al usuario si no los tiene
    private fun verificarYPedirPermisosFocus() {
        // Si no tiene permiso, entra al if
        if (!comprobarPermisoDeUso()) {
            // Crea una alerta visual para avisar al usuario de porque necesita la app dichos permisos, obligatorio por Google Play
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Activar Modo Focus")
            builder.setMessage("Para que Kyro te pueda ayudar a concentrarte y evitar distracciones con redes sociales, necesita permiso para detectar que apps usas. " +
                    "\n\nBusca 'Kyro' en la siguiente lista  y actívalo si estas de acuerdo.")

            // Lleva al usuario a la configuración de Android si selecciona esta opción
            builder.setPositiveButton("Ir a Ajustes") { dialog, _ ->
                // Abre la lista de "Acceso a datos de uso" del sistema
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                // Cierra el dialogo
                dialog.dismiss()
            }

            // En caso de ser negativo, cierra el diálogo
            builder.setNegativeButton("Más tarde") { dialog, _ ->
                dialog.dismiss()
            }

            // Es obligatorio elegir una opción
            builder.setCancelable(false)

            // Muestra el diálogo
            builder.show()
        }
    }
}