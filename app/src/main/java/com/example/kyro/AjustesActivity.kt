package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class AjustesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajustes)

        initListeners()
    }

    private fun initListeners() {
        // Lógica de la flecha para volver atrás
        val btnAtras = findViewById<ImageView>(R.id.btnAtras)
        btnAtras.setOnClickListener {
            finish() // Cierra la pantalla, volviendo a la anterior
        }

        // Lógica de configuración de cuenta (Se profundizara en el futuro)
        val cardCuenta = findViewById<MaterialCardView>(R.id.cardCuenta)
        cardCuenta.setOnClickListener {
            // Muestra el mensaje ir a perfil de usuario, ya que aun no esta creado.
            Toast.makeText(this, "Ir a Perfil de Usuario", Toast.LENGTH_SHORT).show()
        }

        // Botón de cambiar idioma, solo muestra "Seleccionar Idioma" se desarrollara proximamente
        val btnIdioma = findViewById<View>(R.id.btnIdioma)
        btnIdioma.setOnClickListener {
            Toast.makeText(this, "Seleccionar Idioma", Toast.LENGTH_SHORT).show()
        }

        // Igual que el botón anterior, muestra el texto, se desarollara en el futuro
        val btnTema = findViewById<View>(R.id.btnTema)
        btnTema.setOnClickListener {
            Toast.makeText(this, "Cambiar Tema Oscuro/Claro", Toast.LENGTH_SHORT).show()
        }

        // Muestra un diálogo de advertencia para confirmar si el usuario quiere cerrar sesión
        val btnCerrarSesion = findViewById<View>(R.id.btnCerrarSesion)
        btnCerrarSesion.setOnClickListener {
            mostrarDialogoCerrarSesion()
        }

        // Muestra un dialogo de advertencia para confirmar si el usuario quiere eliminar su cuenta
        val btnEliminarCuenta = findViewById<View>(R.id.btnEliminarCuenta)
        btnEliminarCuenta.setOnClickListener {
            mostrarDialogoEliminarCuenta()
        }
    }

    // --- Métodos para mostrar los diálogos ---

    private fun mostrarDialogoCerrarSesion() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Cerrar Sesión")
        builder.setMessage("¿Estás seguro de que quieres salir?")

        // Si el usuario presiona que sí
        builder.setPositiveButton("Sí, salir") { dialog, _ ->
            // Muestra un mensaje de sesión cerrada, se desarollara la lógica en el futuro
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
        }

        // Si el usuario presiona que no, cierra el diálogo
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun mostrarDialogoEliminarCuenta() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Eliminar Cuenta")
        builder.setMessage("Esta acción no se puede deshacer. ¿Borrar todo?")

        // Si el usuario presiona Eliminar, muestra el texto "Cuenta eliminada", la lógica para eliminarla aún no esta
        builder.setPositiveButton("Eliminar") { dialog, _ ->
            Toast.makeText(this, "Cuenta eliminada", Toast.LENGTH_LONG).show()
        }

        // Si el usuario presiona cancelar, cierra el diálogo.
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        // Resalta el botón de Eliminar de color rojo, así indica su riesgo
        val dialog = builder.create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(android.R.color.holo_red_dark))
    }
}