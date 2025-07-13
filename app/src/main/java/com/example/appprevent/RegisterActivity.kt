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
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)

        // Opciones predeterminadas para la pregunta de seguridad
        val preguntas = listOf(
            "¿Tu color favorito?",
            "¿Nombre de tu primera mascota?",
            "¿Ciudad donde naciste?",
            "¿Nombre de tu escuela primaria?"
        )



        btnRegistrar.setOnClickListener {
            val usuario = etUsuario.text.toString().trim()
            val nombre = etNombre.text.toString().trim()
            val apellidos = etApellidos.text.toString().trim()
            val contraseña = etPassword.text.toString().trim()


            // Validación básica
            if (usuario.isEmpty() || nombre.isEmpty() || apellidos.isEmpty() || contraseña.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val url = URL("http://10.203.178.49:3301/api/auth/registro") // Cambia IP si es necesario
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true

                    val json = JSONObject().apply {
                        put("usuario", usuario)
                        put("nombre", nombre)
                        put("apellidos", apellidos)
                        put("contraseña", contraseña)
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
