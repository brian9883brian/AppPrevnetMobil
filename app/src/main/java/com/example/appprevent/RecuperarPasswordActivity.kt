package com.example.appprevent

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class RecuperarPasswordActivity : AppCompatActivity() {

    private lateinit var etUsuario: EditText
    private lateinit var etRespuesta: EditText
    private lateinit var etNuevaPass: EditText
    private lateinit var btnCambiar: Button
    private lateinit var btnVolver: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var cbMostrarPassword: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recuperar_password)

        etUsuario = findViewById(R.id.etUsuario)
        etRespuesta = findViewById(R.id.etRespuesta)
        etNuevaPass = findViewById(R.id.etNuevaPassword)
        btnCambiar = findViewById(R.id.btnCambiarPassword)
        btnVolver = findViewById(R.id.btnVolverLogin)
        progressBar = findViewById(R.id.progressBar)
        cbMostrarPassword = findViewById(R.id.cbMostrarPassword)

        progressBar.visibility = View.GONE

        cbMostrarPassword.setOnCheckedChangeListener { _, isChecked ->
            etNuevaPass.inputType = if (isChecked) {
                android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            etNuevaPass.setSelection(etNuevaPass.text.length)
        }

        btnCambiar.setOnClickListener {
            val usuario = etUsuario.text.toString().trim()
            val respuesta = etRespuesta.text.toString().trim()
            val nuevaPass = etNuevaPass.text.toString().trim()

            if (usuario.isEmpty() || respuesta.isEmpty() || nuevaPass.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnCambiar.isEnabled = false

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val url = URL("https://laniebla-vol2.onrender.com/api/auth/usuarios/$usuario/recuperar")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true

                    val json = JSONObject()
                    json.put("respuestaSecreta", respuesta)
                    json.put("nuevaContraseña", nuevaPass)

                    val writer = OutputStreamWriter(conn.outputStream)
                    writer.write(json.toString())
                    writer.flush()

                    val responseCode = conn.responseCode

                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnCambiar.isEnabled = true

                        when (responseCode) {
                            200 -> {
                                Toast.makeText(applicationContext, "Contraseña actualizada correctamente", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this@RecuperarPasswordActivity, LoginActivity::class.java))
                                finish()
                            }
                            400, 404 -> {
                                Toast.makeText(applicationContext, "Usuario o respuesta incorrecta", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                Toast.makeText(applicationContext, "Error del servidor: $responseCode", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    writer.close()
                    conn.disconnect()
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnCambiar.isEnabled = true
                        Toast.makeText(applicationContext, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        btnVolver.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
