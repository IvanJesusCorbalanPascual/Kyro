package com.example.kyro.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kyro.Archivo
import com.example.kyro.R

/**
 * Clase que maneja la seleccion de archivos subidos por el usuario para hacer preguntas con IA con esos archivos
 *
 */
class SeleccionArchivosAdapter(
    private val listaArchivos: List<Archivo>
) : RecyclerView.Adapter<SeleccionArchivosAdapter.ViewHolder>() {

    // Set para guardar los objetos seleccionados
    val seleccionados = mutableSetOf<Archivo>()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkbox: CheckBox = view.findViewById(R.id.cbArchivoItem)
        val nombre: TextView = view.findViewById(R.id.tvNombreArchivoItem)

        fun bind(archivo: Archivo) { // Le pasamos un archivo y lo mostramos ya chekeado
            nombre.text = archivo.nombre

            // Lógica del checkbox
            checkbox.setOnCheckedChangeListener(null) // Evitar rebotes al reciclar
            checkbox.isChecked = seleccionados.contains(archivo)

            checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) seleccionados.add(archivo)
                else seleccionados.remove(archivo)
            }

            // Permitir clic en tod0 el item para marcar el check
            itemView.setOnClickListener {
                checkbox.isChecked = !checkbox.isChecked
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Necesitas crear un layout simple 'item_seleccion_archivo.xml'
        // con un CheckBox y un TextView horizontalmente
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_seleccion_archivo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listaArchivos[position])
    }

    override fun getItemCount() = listaArchivos.size
}