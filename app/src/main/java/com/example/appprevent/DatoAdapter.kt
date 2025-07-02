package com.example.appprevent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DatoAdapter(private val listaDatos: List<String>) :
    RecyclerView.Adapter<DatoAdapter.DatoViewHolder>() {

    class DatoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDato: TextView = itemView.findViewById(R.id.tvDato)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DatoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dato, parent, false)
        return DatoViewHolder(view)
    }

    override fun onBindViewHolder(holder: DatoViewHolder, position: Int) {
        holder.tvDato.text = listaDatos[position]
    }

    override fun getItemCount(): Int = listaDatos.size
}
