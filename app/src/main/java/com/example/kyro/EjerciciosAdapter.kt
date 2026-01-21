package com.example.kyro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EjerciciosAdapter(
    private var listaEjercicios: List<EjercicioIA>,
    private val onClick: (EjercicioIA) -> Unit
) : RecyclerView.Adapter<EjerciciosAdapter.EjercicioViewHolder>() {

    // Recarga la lista cuando se actualizan los ejercicios desde la BD
    fun actualizarLista(nuevaLista: List<EjercicioIA>) {
        listaEjercicios = nuevaLista
        notifyDataSetChanged()
    }

    // Prepara el diseño del XML
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EjercicioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ejercicio_card, parent, false)
        return EjercicioViewHolder(view)
    }

    // Rellena los datos de cada tarjeta
    override fun onBindViewHolder(holder: EjercicioViewHolder, position: Int) {
        val ejercicio = listaEjercicios[position]

        holder.tvTitulo.text = if (ejercicio.nombre.isNotEmpty()) ejercicio.nombre else "Test Generado ${position + 1}"
        holder.tvDescripcion.text = "Toca para realizar este test"
        // Define la acción al hacer clic en un elemento
        holder.itemView.setOnClickListener {
            onClick(ejercicio)
        }
    }

    // Devuelve la cantidad de elementos que hay en total
    override fun getItemCount(): Int = listaEjercicios.size


    // Clase que se encarga de guardar las referencias a los textos del titulo y descripcion
    class EjercicioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitulo: TextView = itemView.findViewById(R.id.tvTituloEjercicio)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcionEjercicio)
    }
}