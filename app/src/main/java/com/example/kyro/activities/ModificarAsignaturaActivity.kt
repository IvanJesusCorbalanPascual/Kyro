package com.example.kyro.activities

import android.content.Intent
import android.net.Uri // NUEVO
import android.os.Bundle
import android.provider.OpenableColumns // NUEVO
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts // NUEVO
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.kyro.entities.Archivo
import com.example.kyro.NavigationHelper
import com.example.kyro.R
import com.example.kyro.SupabaseClient
import com.example.kyro.showKyroToast
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch

class ModificarAsignaturaActivity : AppCompatActivity() {

    // Para guardar la URI del archivo seleccionado
    private var selectedFileUri: Uri? = null
    private lateinit var btnAdjuntar: TextView

    // Bloque para abrir el selector de archivos
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            val fileName = getFileNameFromUri(uri)
            // Actualizamos el texto del botón para que el usuario sepa que ha seleccionado algo
            btnAdjuntar.text = "\uD83D\uDCC4 $fileName" // Icono de documento + nombre
            btnAdjuntar.setTextColor(getColor(R.color.b500)) // Azul Kyro (asegurate de tener este color o usa otro)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modificar_asignatura)

        val etContenido = findViewById<EditText>(R.id.etContenidoApuntes)
        val etTitulo = findViewById<EditText>(R.id.etTituloModificar)
        val btnAnalizar = findViewById<MaterialButton>(R.id.btnAnalizar) // Botón Guardar
        val btnEliminar = findViewById<MaterialButton>(R.id.btnEliminar)

        // Inicializamos la variable global btnAdjuntar
        btnAdjuntar = findViewById(R.id.btnAdjuntar)

        val tvTituloTarjeta = findViewById<TextView>(R.id.tvNombreAsignatura)

        val contenidoOriginal = intent.getStringExtra("EXTRA_CONTENIDO") ?: ""
        val tituloOriginal = intent.getStringExtra("EXTRA_TITULO") ?: ""
        val idRecibido = intent.getLongExtra("EXTRA_ID", -1)

        etContenido.setText(contenidoOriginal)
        etTitulo.setText(tituloOriginal)
        tvTituloTarjeta.text = tituloOriginal

        // Lógica del botón adjuntar (ahora abre el explorador)
        btnAdjuntar.setOnClickListener {
            // Abre el selector para cualquier tipo de archivo
            filePickerLauncher.launch("*/*")
        }

        btnAnalizar.setOnClickListener {
            val contenidoTexto = etContenido.text.toString().trim()
            val tituloTexto = etTitulo.text.toString().trim()

            if (tituloTexto.isEmpty()) {
                etTitulo.error = getString(R.string.error_titulo_vacio)
                mostrarMensaje(getString(R.string.toast_falta_titulo))
            } else {
                mostrarMensaje("Guardando cambios...")

                btnAnalizar.isEnabled = false
                btnAnalizar.text = getString(R.string.btn_estado_guardando)

                lifecycleScope.launch {
                    try {
                        // Actualizar Texto (Título y Contenido) en la tabla 'asignaturas'
                        SupabaseClient.client
                            .from("asignaturas")
                            .update({
                                set("contenido", contenidoTexto)
                                set("titulo", tituloTexto)
                            }) {
                                filter {
                                    eq("id", idRecibido)
                                }
                            }

                        // Si hay un archivo seleccionado, subirlo y vincularlo
                        if (selectedFileUri != null) {
                            subirArchivoSeleccionado(idRecibido)
                        }

                        mostrarMensaje(getString(R.string.toast_asignatura_actualizada))
                        volverALaLista()

                    } catch (e: Exception) {
                        e.printStackTrace()
                        mostrarMensaje(getString(R.string.error_actualizar_msg, e.message))
                        btnAnalizar.isEnabled = true
                        btnAnalizar.text = getString(R.string.mod_asignatura_btn_guardar)
                    }
                }
            }
        }

        btnEliminar.setOnClickListener {
            confirmarEliminacion(idRecibido)
        }

        NavigationHelper.setupBottomNavigation(this, R.id.nav_asignatura)
    }

    // Función suspendida para manejar la subida del archivo (extraída para limpieza)
    private suspend fun subirArchivoSeleccionado(asignaturaId: Long) {
        val usuarioActual = SupabaseClient.client.auth.currentUserOrNull() ?: return

        val nombreArchivo = getFileNameFromUri(selectedFileUri!!)
        // Leemos los bytes del archivo
        val datosArchivo = contentResolver.openInputStream(selectedFileUri!!)?.readBytes()

        if (datosArchivo != null) {
            val nombreLimpio = arreglarNombreArchivo(nombreArchivo)
            // Ruta en el Storage: ID_USUARIO / TIMESTAMP_NOMBRE
            val nombreEnNube = "${usuarioActual.id}/${System.currentTimeMillis()}_$nombreLimpio"

            // Referencia al bucket "Apuntes" (debe coincidir con Supabase)
            val bucket = SupabaseClient.client.storage.from("Apuntes")

            // Subir archivo
            bucket.upload(nombreEnNube, datosArchivo, upsert = false)

            // Obtener URL pública
            val urlPublica = bucket.publicUrl(nombreEnNube)

            // Guardar referencia en la tabla 'archivos'
            val nuevoArchivo = Archivo(
                userId = usuarioActual.id,
                asignaturaId = asignaturaId,
                nombre = nombreArchivo,
                url = urlPublica
            )
            SupabaseClient.client.from("archivos").insert(nuevoArchivo)
        }
    }

    // Helper para obtener nombre del archivo
    private fun getFileNameFromUri(uri: Uri): String {
        var name = "Archivo adjunto"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }

    private fun arreglarNombreArchivo(nombreOriginal: String): String {
        return nombreOriginal
            .replace(" ", "_")
            .replace(Regex("[^a-zA-Z0-9._-]"), "")
    }

    private fun confirmarEliminacion(id: Long) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.dialog_eliminar_asignatura_titulo))
        builder.setMessage(getString(R.string.dialog_eliminar_asignatura_msg))

        builder.setPositiveButton(getString(R.string.btn_eliminar)) { dialog, _ ->
            borrarAsignaturaDeSupabase(id)
            dialog.dismiss()
        }

        builder.setNegativeButton(getString(R.string.btn_cancelar)) { dialog, _ ->
            dialog.dismiss()
        }

        val alert = builder.create()
        alert.show()
        alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(android.R.color.holo_red_dark, theme))
    }

    private fun borrarAsignaturaDeSupabase(id: Long) {
        val btnEliminar = findViewById<MaterialButton>(R.id.btnEliminar)
        btnEliminar.isEnabled = false
        btnEliminar.text = getString(R.string.btn_estado_borrando)

        lifecycleScope.launch {
            try {
                SupabaseClient.client
                    .from("asignaturas")
                    .delete {
                        filter {
                            eq("id", id)
                        }
                    }

                mostrarMensaje(getString(R.string.toast_asignatura_eliminada))
                volverALaLista()

            } catch (e: Exception) {
                mostrarMensaje(getString(R.string.error_eliminar_generico, e.message))
                btnEliminar.isEnabled = true
                btnEliminar.text = getString(R.string.mod_asignatura_btn_eliminar)
            }
        }
    }

    private fun volverALaLista() {
        val intent = Intent(this, AsignaturaActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    private fun mostrarMensaje(mensaje: String) {
        showKyroToast(mensaje)
    }
}