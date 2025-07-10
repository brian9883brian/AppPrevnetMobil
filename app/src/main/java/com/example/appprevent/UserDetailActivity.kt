package com.example.appprevent

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.ImageView

class UserDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_detail)

        // Referencias a las vistas
        val txtNombre = findViewById<TextView>(R.id.txtNombre)
        val txtCorreo = findViewById<TextView>(R.id.txtCorreo)
        val txtTelefono = findViewById<TextView>(R.id.txtTelefono)
        val txtDireccion = findViewById<TextView>(R.id.txtDireccion)
        val userImage = findViewById<ImageView>(R.id.userImage)

        // Datos estáticos (más adelante los cambiarás por datos desde la API)
        txtNombre.text = "Nombre: Eduardo Mendoza"
        txtCorreo.text = "Correo: eduardo@example.com"
        txtTelefono.text = "Teléfono: 5512345678"
        txtDireccion.text = "Dirección: Calle Falsa 123, CDMX"
        userImage.setImageResource(R.drawable.ic_user)
    }
}
