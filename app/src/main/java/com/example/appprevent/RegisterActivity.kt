package com.example.appprevent

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etUsuario = findViewById<EditText>(R.id.etUsuario)
        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etApellidos = findViewById<EditText>(R.id.etApellidos)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etTelefono = findViewById<EditText>(R.id.etTelefono)
        val etDomicilio = findViewById<EditText>(R.id.etDomicilio)
        val etFamiliar = findViewById<EditText>(R.id.etFamiliar)
        val etPregunta = findViewById<EditText>(R.id.etPregunta)         // NUEVO
        val etRespuesta = findViewById<EditText>(R.id.etRespuesta)       // NUEVO

        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)

        btnRegistrar.setOnClickListener {
            val usuario = etUsuario.text.toString().trim()
            val nombre = etNombre.text.toString().trim()
            val apellidos = etApellidos.text.toString().trim()
            val contraseña = etPassword.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val telefono = etTelefono.text.toString().trim()
            val domicilio = etDomicilio.text.toString().trim()
            val familiar = etFamiliar.text.toString().trim()
            val preguntaSecreta = etPregunta.text.toString().trim()          // NUEVO
            val respuestaSecreta = etRespuesta.text.toString().trim()        // NUEVO

            // Validación básica (agregados pregunta y respuesta secreta)
            if (usuario.isEmpty() || nombre.isEmpty() || apellidos.isEmpty() || contraseña.isEmpty() ||
                correo.isEmpty() || telefono.isEmpty() || domicilio.isEmpty() || familiar.isEmpty() ||
                preguntaSecreta.isEmpty() || respuestaSecreta.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val url = URL("https://laniebla-vol2.onrender.com/api/auth/registro") // Cambia si es necesario
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true

                    val json = JSONObject().apply {
                        put("usuario", usuario)
                        put("nombre", nombre)
                        put("apellidos", apellidos)
                        put("contraseña", contraseña)
                        put("correo", correo)
                        put("telefono", telefono)
                        put("domicilio", domicilio)
                        put("familiar", familiar)
                        put("preguntaSecreta", preguntaSecreta)       // NUEVO
                        put("respuestaSecreta", respuestaSecreta)     // NUEVO
                    }

                    Log.d("RegistroJSON", json.toString())

                    val output = OutputStreamWriter(conn.outputStream)
                    output.write(json.toString())
                    output.flush()

                    val responseCode = conn.responseCode

                    withContext(Dispatchers.Main) {
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            Toast.makeText(applicationContext, "Registro exitoso", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(applicationContext, "Error al registrar: Código $responseCode", Toast.LENGTH_LONG).show()
                        }
                    }

                    conn.disconnect()
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
