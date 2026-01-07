package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class ModificarTemarioActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modificar_temario)

        // Conenexiones de elementos, los busca en el XML por id para usarlos en la lógica
        val etContenido = findViewById<EditText>(R.id.etContenidoApuntes)
        val btnAnalizar = findViewById<MaterialButton>(R.id.btnAnalizar)
        val btnAdjuntar = findViewById<TextView>(R.id.btnAdjuntar)

        // Lógica de los botones

        btnAdjuntar.setOnClickListener {
            mostrarMensaje("Funcionalidad para subir el PDF aún esta desarrollandose")
        }

        btnAnalizar.setOnClickListener {
            // Coge el texto y lo almacena quitando espacios sobrantes
            val contenidoTexto = etContenido.text.toString().trim()

            // Comprueba si esta vacio el edit text de los apuntes
            if (contenidoTexto.isEmpty()) {
                // Marca un error visual en la caja si no hay texto y avisa al usuario
                etContenido.error = "Debes pegar el texto del temario aquí"
                mostrarMensaje("El campo de texto está vacío")

            // Ha funcionado correctamente habiendo detectado que había texto
            } else {
                mostrarMensaje("Procesando contenido...")

                // Vuelve a la pantalla anterior de temario seleccionado
                val intent = Intent(this, TemarioSeleccionadoActivity::class.java)
                startActivity(intent)

                // Cierra la pantalla
                finish()

            }
        }


    }
    // Función para mostrar los mensajes y no tener que escribir Toast.makeText constantemente
    private fun mostrarMensaje(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }
}