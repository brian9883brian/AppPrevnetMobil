package com.example.appprevent

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.view.animation.AnimationUtils
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class LoginActivity : AppCompatActivity() {

    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        username = findViewById(R.id.etEmail)
        password = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)

        val anim = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        username.startAnimation(anim)
        password.startAnimation(anim)
        btnLogin.startAnimation(anim)

        val btnRegister = findViewById<Button>(R.id.btnRegister)
        btnRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }


        btnLogin.setOnClickListener {
            val user = username.text.toString().trim()
            val pass = password.text.toString().trim()

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            iniciarSesion(user, pass)
        }
    }

    private fun iniciarSesion(usuario: String, contraseña: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("http://10.203.178.49:3301/api/auth/login") // ← Cambia IP si no estás en emulador
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val json = JSONObject()
                json.put("usuario", usuario)
                json.put("contraseña", contraseña)

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(json.toString())
                writer.flush()

                val responseCode = conn.responseCode

                if (responseCode == 200) {
                    val responseStream = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(responseStream)
                    val mensaje = jsonResponse.getString("mensaje")

                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, mensaje, Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                    }
                }

                writer.close()
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}