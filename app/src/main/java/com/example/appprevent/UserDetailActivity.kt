package com.example.appprevent

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.ImageView

class UserDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_detail)

        val txtNombre = findViewById<TextView>(R.id.txtNombre)
        val txtCorreo = findViewById<TextView>(R.id.txtCorreo)
        val txtTelefono = findViewById<TextView>(R.id.txtTelefono)
        val txtDireccion = findViewById<TextView>(R.id.txtDireccion)
        val userImage = findViewById<ImageView>(R.id.userImage)

        // Leer datos guardados en SharedPreferences
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        val nombre = prefs.getString("usuario_nombre", "No definido")
        val correo = prefs.getString("usuario_correo", "No definido")
        val telefono = prefs.getString("usuario_telefono", "No definido")
        val direccion = prefs.getString("usuario_domicilio", "No definido") // Si lo guardaste en login o registro

        txtNombre.text = "Nombre: $nombre"
        txtCorreo.text = "Correo: $correo"
        txtTelefono.text = "Teléfono: $telefono"
        txtDireccion.text = "Dirección: $direccion"

        userImage.setImageResource(R.drawable.ic_user)
    }
}
