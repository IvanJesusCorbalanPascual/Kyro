package com.example.kyro.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kyro.EjercicioIA
import com.example.kyro.R

class EjerciciosAdapter(
    private var listaEjercicios: List<EjercicioIA>,
    private val onClick: (EjercicioIA) -> Unit,
    private val onDelete: (EjercicioIA) -> Unit
) : RecyclerView.Adapter<EjerciciosAdapter.EjercicioViewHolder>() {

    fun actualizarLista(nuevaLista: List<EjercicioIA>) {
        listaEjercicios = nuevaLista
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EjercicioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ejercicio_card, parent, false)
        return EjercicioViewHolder(view)
    }

    override fun onBindViewHolder(holder: EjercicioViewHolder, position: Int) {
        val ejercicio = listaEjercicios[position]

        holder.tvTitulo.text = if (ejercicio.nombre.isNotEmpty()) ejercicio.nombre else "Test Generado ${position + 1}"
        holder.tvDescripcion.text = "Toca para realizar este test"

        // Clic en la tarjeta para abrir
        holder.itemView.setOnClickListener {
            onClick(ejercicio)
        }

        // Clic en la papelera para borrar
        holder.btnEliminar.setOnClickListener {
            onDelete(ejercicio)
        }
    }

    override fun getItemCount(): Int = listaEjercicios.size

    class EjercicioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitulo: TextView = itemView.findViewById(R.id.tvTituloEjercicio)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcionEjercicio)
        val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminar)
    }
}