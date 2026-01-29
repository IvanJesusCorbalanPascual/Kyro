package com.example.kyro.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kyro.entities.Archivo
import com.example.kyro.entities.Asignatura
import com.example.kyro.adapters.AsignaturaAdapter
import com.example.kyro.NavigationHelper
import com.example.kyro.R
import com.example.kyro.SupabaseClient
import com.google.android.material.button.MaterialButton
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import io.github.jan.supabase.storage.storage

class AsignaturaActivity : AppCompatActivity() {

    // Variables globales
    private lateinit var etTituloNuevo: EditText
    private lateinit var etContenidoNuevo: EditText
    private lateinit var btnGuardar: MaterialButton // Renombrado para claridad
    private lateinit var rvAsignaturas: RecyclerView
    private lateinit var barraProgreso: ProgressBar
    private lateinit var tvVacio: TextView

    private lateinit var btnAdjuntar: TextView

    // Guarda la URI del archivo seleccionado
    private var selectedFileUri: Uri? = null

    // Bloque para abrir documentos
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            val fileName = getFileNameFromUri(uri)
            btnAdjuntar.text = "\uD83D\uDCC4 $fileName"
            btnAdjuntar.setTextColor(getColor(R.color.b500))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asignatura)

        PDFBoxResourceLoader.init(applicationContext)

        // Variables vinculadas
        etTituloNuevo = findViewById(R.id.etTituloNuevoAsignatura)
        etContenidoNuevo = findViewById(R.id.etContenidoNuevoAsignatura)
        btnGuardar = findViewById(R.id.btnGenerar) // Mantengo el ID del XML aunque cambie la variable
        rvAsignaturas = findViewById(R.id.rvAsignaturas)
        tvVacio = findViewById(R.id.tvVacio)
        barraProgreso = findViewById(R.id.barraProgreso)
        btnAdjuntar = findViewById(R.id.btnAdjuntarArchivo)

        btnAdjuntar.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }

        cargarAsignaturas()
        setupGuardarButton()
    }

    override fun onResume() {
        super.onResume()
        NavigationHelper.setupBottomNavigation(this, R.id.nav_asignatura)
    }

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

    private fun cargarAsignaturas() {
        barraProgreso.visibility = View.VISIBLE
        tvVacio.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val usuarioActual = SupabaseClient.client.auth.currentUserOrNull()

                if (usuarioActual != null) {
                    val listaAsignaturas = SupabaseClient.client
                        .from("asignaturas")
                        .select {
                            filter {
                                eq("user_id", usuarioActual.id)
                            }
                        }
                        .decodeList<Asignatura>()
                        .sortedByDescending { it.id }

                    barraProgreso.visibility = View.GONE

                    if (listaAsignaturas.isEmpty()) {
                        tvVacio.visibility = View.VISIBLE
                        rvAsignaturas.visibility = View.GONE
                    } else {
                        tvVacio.visibility = View.GONE
                        rvAsignaturas.visibility = View.VISIBLE
                        rvAsignaturas.layoutManager = LinearLayoutManager(this@AsignaturaActivity)
                        rvAsignaturas.adapter = AsignaturaAdapter(listaAsignaturas) { asignatura ->
                            abrirDetalle(asignatura)
                        }
                    }
                } else {
                    barraProgreso.visibility = View.GONE
                    tvVacio.text = "Error de conexion."
                    tvVacio.visibility = View.VISIBLE
                }

            }catch (e: Exception) {
                barraProgreso.visibility = View.GONE
                Log.e("AsignaturaActivity", "Error al cargar" , e)
                showKyroToast("Error de conexión")
            }
        }
    }

    private fun setupGuardarButton() {
        btnGuardar.setOnClickListener {
            val titulo = etTituloNuevo.text.toString().trim()
            val contenido = etContenidoNuevo.text.toString().trim()

            if (titulo.isEmpty()) {
                etTituloNuevo.error = "Escribe el título, es obligatorio"
                return@setOnClickListener
            }

            // Validacion simple: titulo obligatorio. El contenido puede ser opcional si solo quieren crear la carpeta
            // Pero mantendremos tu logica de requerir algo de contenido o archivo
            if (contenido.isEmpty() && selectedFileUri == null) {
                etContenidoNuevo.error = "Escribe contenido o adjunta un PDF"
                showKyroToast("Debes añadir contenido para guardar la asignatura")
                return@setOnClickListener
            }

            guardarNuevaAsignatura(titulo, contenido)
        }
    }

    // Guardar Asignatura
    private fun guardarNuevaAsignatura(titulo: String, contenido: String) {
        btnGuardar.isEnabled = false
        btnGuardar.text = "Guardando..."

        lifecycleScope.launch {
            try {
                val usuarioActual = SupabaseClient.client.auth.currentUserOrNull()

                if (usuarioActual == null) {
                    showKyroToast("Error: La sesión no es válida")
                    return@launch
                }

                val contenidoParaGuardar = contenido.ifEmpty {
                    "📁 Archivo adjunto: ${getFileNameFromUri(selectedFileUri ?: Uri.EMPTY)}"
                }

                // Guardar la Asignatura en Supabase
                val nuevaAsignatura = Asignatura(
                    titulo = titulo,
                    contenido = contenidoParaGuardar,
                    user_id = usuarioActual.id
                )

                val asignaturaGuardada = SupabaseClient.client
                    .from("asignaturas")
                    .insert(nuevaAsignatura) {
                        select()
                    }.decodeSingle<Asignatura>()

                // Si hay archivo, subirlo y vincularlo
                if (selectedFileUri != null) {
                    val nombreArchivo = getFileNameFromUri(selectedFileUri!!)
                    val datosArchivo = contentResolver.openInputStream(selectedFileUri!!)?.readBytes()

                    if (datosArchivo != null) {
                        val nombreLimpio = arreglarNombreArchivo(nombreArchivo)
                        val nombreEnNube = "${usuarioActual.id}/${System.currentTimeMillis()}_$nombreLimpio"
                        val bucket = SupabaseClient.client.storage.from("Apuntes")

                        bucket.upload(nombreEnNube, datosArchivo, upsert = false)
                        val urlPublica = bucket.publicUrl(nombreEnNube)

                        val nuevoArchivo = Archivo(
                            userId = usuarioActual.id,
                            asignaturaId = asignaturaGuardada.id,
                            nombre = nombreArchivo,
                            url = urlPublica
                        )
                        SupabaseClient.client.from("archivos").insert(nuevoArchivo)
                    }
                }

                // --- FIN DEL PROCESO ---
                // Ya NO llamamos a la IA ni creamos ejercicios automáticos.

                showKyroToast("¡Asignatura guardada correctamente!")

                // Limpieza de UI
                etTituloNuevo.text.clear()
                etContenidoNuevo.text.clear()
                btnAdjuntar.text = "Pulse aquí para adjuntar archivos"
                btnAdjuntar.setTextColor(getColor(R.color.black))
                selectedFileUri = null
                etTituloNuevo.clearFocus()
                etContenidoNuevo.clearFocus()

                // Recargar lista
                delay(500)
                cargarAsignaturas()

            } catch (e: Exception) {
                Log.e("AsignaturaActivity", "Error al guardar", e)
                showKyroToast("Error al guardar la asignatura")
            } finally {
                btnGuardar.isEnabled = true
                btnGuardar.text = "Guardar Asignatura" // O el texto que prefieras en tu XML
            }
        }
    }

    private fun arreglarNombreArchivo(nombreOriginal: String): String {
        return nombreOriginal
            .replace(" ", "_")
            .replace(Regex("[^a-zA-Z0-9._-]"), "")
    }

    private fun showKyroToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun abrirDetalle(asignatura: Asignatura) {
        val intent = Intent(this, AsignaturaSeleccionadaActivity::class.java)
        intent.putExtra("EXTRA_TITULO", asignatura.titulo)
        intent.putExtra("EXTRA_CONTENIDO", asignatura.contenido)
        intent.putExtra("EXTRA_ID", asignatura.id)
        startActivity(intent)
    }
}