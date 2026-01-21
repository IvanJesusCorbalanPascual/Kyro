package com.example.kyro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import java.util.ArrayList // Necesario para pasar la lista

class AsignaturaActivity : AppCompatActivity() {

    // Variables globales, todas las funciones pueden acceder a ellas
    private lateinit var etTituloNuevo: EditText
    private lateinit var etContenidoNuevo: EditText
    private lateinit var btnGenerar: MaterialButton
    private lateinit var rvAsignaturas: RecyclerView
    private lateinit var barraProgreso: ProgressBar
    private lateinit var tvVacio: TextView

    private lateinit var btnAdjuntar: TextView

    // Guarda la URI del archivo seleccionado
    private var selectedFileUri: Uri? = null

    // Bloque para abrir documentos
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // Si el usuario seleccionó un archivo correctamente
        if (uri != null) {
            selectedFileUri = uri
            val fileName = getFileNameFromUri(uri)

            // Actualiza el texto del botón visualmente
            btnAdjuntar.text = "\uD83D\uDCC4 $fileName"
            // Pone el color azul en caso de éxito
            btnAdjuntar.setTextColor(getColor(R.color.b500))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asignatura)

        // Inicia la librería de PDF, para que se pueden cargar sus recursos
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(applicationContext)

        // Variables vinculadas con los IDs del XML
        etTituloNuevo = findViewById(R.id.etTituloNuevoAsignatura)
        etContenidoNuevo = findViewById(R.id.etContenidoNuevoAsignatura)
        btnGenerar = findViewById(R.id.btnGenerar)
        rvAsignaturas = findViewById(R.id.rvAsignaturas)
        tvVacio = findViewById(R.id.tvVacio)
        barraProgreso = findViewById(R.id.barraProgreso)
        btnAdjuntar = findViewById(R.id.btnAdjuntarArchivo)

        // Configura el botón de adjuntar
        btnAdjuntar.setOnClickListener {
            // Lanza el selector para buscar PDFs
            filePickerLauncher.launch("*/*")
        }

        // Carga la lista al entrar
        cargarAsignaturas()

        // Configura el botón que permite cargar nuevos temas
        setupGenerarButton()
    }

    override fun onResume() {
        super.onResume()
        // Llama al Helper y le dice que ilumine asignatura
        NavigationHelper.setupBottomNavigation(this, R.id.nav_asignatura)
    }

    // Función auxiliar, ayuda a obtener el nombre real del archivo
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

    // Carga los datos desde Supabase filtrando por usuario
    private fun cargarAsignaturas() {
        // Muestra la rueda de carga y oculta el mensaje vacio
        barraProgreso.visibility = View.VISIBLE
        tvVacio.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Obtiene el usuario conectado en la sesion actual
                val usuarioActual = SupabaseClient.client.auth.currentUserOrNull()

                // Pide los datos de la tabla "asignaturas" del usuario actual
                if (usuarioActual != null) {
                    // Pide las asignaturas aplicando el filtro de usuario
                    val listaAsignaturas = SupabaseClient.client
                        .from("asignaturas")
                        .select {
                            // Solo trae el user_id que es igual al id
                            filter {
                                eq("user_id", usuarioActual.id)
                            }
                        }
                        // Convierte a objetos Kotlin el JSON
                        .decodeList<Asignatura>()
                        // Lo más nuevo aparece primero
                        .sortedByDescending { it.id }

                    // Oculta la carga al terminar
                    barraProgreso.visibility = View.GONE

                    // Comprueba si esta vacia o llena la lista
                    if (listaAsignaturas.isEmpty()) {
                        // Muestra el aviso de que no hay temas, si esta vacio
                        tvVacio.visibility = View.VISIBLE
                        rvAsignaturas.visibility = View.GONE
                    } else {
                        tvVacio.visibility = View.GONE
                        rvAsignaturas.visibility = View.VISIBLE

                        // Configura el layout del RecyclerView
                        rvAsignaturas.layoutManager = LinearLayoutManager(this@AsignaturaActivity)

                        // Conecta el adaptador con la lista de datos
                        rvAsignaturas.adapter = AsignaturaAdapter(listaAsignaturas) { asignatura ->
                            // Se ejecuta al pulsar una tarjeta, abriendo el detalle
                            abrirDetalle(asignatura)
                        }
                    }
                } else {
                    // En el caso improbable de que no haya usuario logeado, oculta
                    barraProgreso.visibility = View.GONE
                    tvVacio.text = "Error de sesión."
                    tvVacio.visibility = View.VISIBLE
                }

            }catch (e: Exception) {
                // Si falla por falta de internet, etc, quita la carga y avisa del error
                barraProgreso.visibility = View.GONE
                Log.e("AsignaturaActivity", "Error al cargar" , e)
                showKyroToast("Error de conexión")
            }
        }
    }

    // Configura el botón para generar ejercicios
    private fun setupGenerarButton() {
        btnGenerar.setOnClickListener {
            val titulo = etTituloNuevo.text.toString().trim()
            val contenido = etContenidoNuevo.text.toString().trim()

            // Si no hay texto, avisa al usuario
            if (titulo.isEmpty()) {
                etTituloNuevo.error = "Escribe el título, es obligatorio"
                return@setOnClickListener
            }

            // Permite generar si hay contenido escrito o si hay un archivo adjunto
            if (contenido.isEmpty() && selectedFileUri == null) {
                etContenidoNuevo.error = "Escribe contenido o adjunta un PDF"
                showKyroToast("Debes añadir contenido para generar ejercicios")
                return@setOnClickListener
            }

            // Establece un minimo de 50 caracteres para evitar fallos o respuestas muy pobres
            if (contenido.isNotEmpty() && contenido.length < 50) {
                etContenidoNuevo.error = "Escribe al menos 50 caracteres o adjunta un PDF"
                showKyroToast("El contenido es muy corto para la IA, 50 carácteres o más requeridos")
                return@setOnClickListener
            }

            // Si están ambos datos, llama a la función para subirlo y procesarlo con IA
            subirNuevaAsignaturaYGenerar(titulo, contenido)
        }
    }

    // Función que envia los datos a la nube
    private fun subirNuevaAsignaturaYGenerar(titulo: String, contenido: String) {
        // Bloquea el botón para no pulsarse más veces
        btnGenerar.isEnabled = false
        btnGenerar.text = "Subiendo y analizando..."

        lifecycleScope.launch {
            try {
                // Identifica el usuario actual en la sesión
                val usuarioActual = SupabaseClient.client.auth.currentUserOrNull()

                // Si no hay usuario por alguna razón, corta sesión para evitar errores
                if (usuarioActual == null) {
                    showKyroToast("Error: La sesión no es válida")
                    return@launch
                }

                // Si el usuario deja el texto vacío, lo rellena automáticamente para evitar fallos en la BD
                val contenidoParaGuardar = contenido.ifEmpty {
                    "📁 Archivo adjunto: ${getFileNameFromUri(selectedFileUri ?: Uri.EMPTY)}"
                }
                // Guarda la asignatura primero, para obtener su ID y poder vincular el archivo despues
                val nuevaAsignatura = Asignatura(titulo = titulo, contenido = contenidoParaGuardar, user_id = usuarioActual.id)

                val asignaturaGuardada = SupabaseClient.client
                    .from("asignaturas")
                    .insert(nuevaAsignatura) {
                        select()
                    }.decodeSingle<Asignatura>()

                // Si hay un archivo adjunto, aqui leera su texto
                var textoParaLaIA = contenido

                if (selectedFileUri != null) {
                    // Lee el archivo para subirlo a la nube de Supabase
                    val nombreArchivo = getFileNameFromUri(selectedFileUri!!)
                    val datosArchivo = contentResolver.openInputStream(selectedFileUri!!)?.readBytes()

                    if (datosArchivo != null) {

                        // Limpia el nomnbre del archivo antes de usarlo
                        val nombreLimpio = arreglarNombreArchivo(nombreArchivo)

                        // Prepara para subir a Supabase Storage en el bucket "Apuntes" con una ruta unica
                        val nombreEnNube = "${usuarioActual.id}/${System.currentTimeMillis()}_$nombreLimpio"

                        val bucket = SupabaseClient.client.storage.from("Apuntes")

                        // Aquí se sube de verdad a la BD
                        bucket.upload(nombreEnNube, datosArchivo, upsert = false)

                        // Obtiene la URL pública
                        val urlPublica = bucket.publicUrl(nombreEnNube)

                        val nuevoArchivo = Archivo(
                            userId = usuarioActual.id,
                            asignaturaId = asignaturaGuardada.id,
                            nombre = nombreArchivo,
                            url = urlPublica
                        )
                        SupabaseClient.client.from("archivos").insert(nuevoArchivo)

                        // Lee el texto del PDF para la IA usando FileTextExtractor
                        val textoDelPDF = FileTextExtractor.leerContenidoArchivo(this@AsignaturaActivity, selectedFileUri!!)

                        if (textoDelPDF.isNotEmpty()) {
                            // Si detecta texto, lo añade a lo que se envia a la IA
                            textoParaLaIA += "\n\n--- TEXTO EXTRAÍDO DEL ARCHIVO ADJUNTO ($nombreArchivo) ---\n$textoDelPDF"
                        } else {
                            // Si es una imagen o no puede leerlo, avisa
                            textoParaLaIA += "\n(El usuario adjuntó el archivo $nombreArchivo pero no tiene texto seleccionable. Usa tus conocimientos sobre '$titulo'.)"
                        }
                    }
                }

                // Si no hay problemas, muestra al usuario que se ha guardado correctamente
                showKyroToast("¡Guardado! Consultando a la IA...")

                // Limpia ambos campos
                etTituloNuevo.text.clear()
                etContenidoNuevo.text.clear()

                // Resetea el boton
                btnAdjuntar.text = "Pulse aquí para adjuntar archivos"
                // Resetea el color original
                btnAdjuntar.setTextColor(getColor(R.color.black))
                // Limpia la URI
                selectedFileUri = null

                // Quita el foco, hace que baje el teclado
                etTituloNuevo.clearFocus()
                etContenidoNuevo.clearFocus()

                // --- LÓGICA DE IA ---
                val servicioIA = GeminiService()
                val preguntasGeneradas = servicioIA.generarTestDeApuntes(textoParaLaIA)

                if (preguntasGeneradas.isNotEmpty()) {

                    // Guardamos las preguntas en Supabase para que no se pierdan
                    val gson = com.google.gson.Gson()
                    val preguntasJson = gson.toJson(preguntasGeneradas)

                    // Usa la clase ejercicioIA
                    val nuevoEjercicio = EjercicioIA(
                        // Id de la asignatura
                        asignatura_id = asignaturaGuardada.id,
                        nombre = "Test Generado con IA",
                        preguntas_json = preguntasJson
                    )
                    SupabaseClient.client
                        .from("ejercicios")
                        .insert(nuevoEjercicio)

                    // Si la IA funcionó, vamos directos al Quiz
                    val intent = Intent(this@AsignaturaActivity, QuizActivity::class.java)
                    intent.putExtra("EXTRA_PREGUNTAS", ArrayList(preguntasGeneradas))
                    startActivity(intent)
                } else {
                    showKyroToast("La IA no pudo generar preguntas. Revisa el texto.")
                }

                // Espera medio segundo para que la BD pueda procesar los datos
                delay(500)
                // Recarga la lista para que se actualice sola
                cargarAsignaturas()

            } catch (e: Exception) {
                Log.e("AsignaturaActivity", "Error al subir", e)
               showKyroToast("Error al guardar o generar")
            } finally {
                // Siempre, haya error o no, reactiva el botón y el texto del botón
                btnGenerar.isEnabled = true
                btnGenerar.text = "Generar Ejercicios con IA ✨"
            }
        }
    }

    // Función para limpiar nombres de archivo que den problemas a Supabase
    private fun arreglarNombreArchivo(nombreOriginal: String): String {
        return nombreOriginal
            // Cambia los espacios por guiones bajos
            .replace(" ", "_")
            // Elimina lo que no sea letra, numeros, puntos o guiones.
            .replace(Regex("[^a-zA-Z0-9._-]"), "")
    }

    // Función que permite ir a la vista detallada pasando datos
    private fun abrirDetalle(asignatura: Asignatura) {
        val intent = Intent(this, AsignaturaSeleccionadaActivity::class.java)

        // Mete el contenido en el intent antes de enviarlo a la siguiente pantalla (AsignaturaSeleccionadoActivity)
        intent.putExtra("EXTRA_TITULO", asignatura.titulo)
        intent.putExtra("EXTRA_CONTENIDO", asignatura.contenido)
        intent.putExtra("EXTRA_ID", asignatura.id)

        startActivity(intent)
    }
}