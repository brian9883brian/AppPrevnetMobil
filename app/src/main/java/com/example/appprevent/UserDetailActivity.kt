package com.example.appprevent

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.ImageView
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class UserDetailActivity : AppCompatActivity() {

    private lateinit var txtNombre: TextView
    private lateinit var txtApellidos: TextView
    private lateinit var txtUsuario: TextView
    private lateinit var txtCorreo: TextView
    private lateinit var txtTelefono: TextView
    private lateinit var txtDomicilio: TextView
    private lateinit var txtFamiliar: TextView
    private lateinit var txtGuid: TextView
    private lateinit var txtPreguntaSecreta: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_detail)

        // Referencias a los TextViews en el layout (debes agregarlos)
        txtNombre = findViewById(R.id.txtNombre)
        txtApellidos = findViewById(R.id.txtApellidos)
        txtUsuario = findViewById(R.id.txtUsuario)
        txtCorreo = findViewById(R.id.txtCorreo)
        txtTelefono = findViewById(R.id.txtTelefono)
        txtDomicilio = findViewById(R.id.txtDomicilio)
        txtFamiliar = findViewById(R.id.txtFamiliar)
        txtGuid = findViewById(R.id.txtGuid)
        txtPreguntaSecreta = findViewById(R.id.txtPreguntaSecreta)

        val userImage = findViewById<ImageView>(R.id.userImage)
        userImage.setImageResource(R.drawable.ic_user)

        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val guid = intent.getStringExtra("guid") ?: prefs.getString("usuario_guid", "") ?: ""

        txtGuid.text = "GUID: $guid"

        if (guid.isNotEmpty()) {
            cargarDatosDesdeApi(guid)
        } else {
            // Mostrar datos guardados en SharedPreferences si no hay guid
            txtNombre.text = "Nombre: ${prefs.getString("usuario_nombre", "No definido")}"
            txtCorreo.text = "Correo: ${prefs.getString("usuario_correo", "No definido")}"
            txtTelefono.text = "Teléfono: ${prefs.getString("usuario_telefono", "No definido")}"
            txtDomicilio.text = "Domicilio: ${prefs.getString("usuario_direccion", "No definido")}"
            txtGuid.text = "GUID: No definido"
            // Limpiar otros campos
            txtApellidos.text = ""
            txtUsuario.text = ""
            txtFamiliar.text = ""
            txtPreguntaSecreta.text = ""
        }
    }

    private fun cargarDatosDesdeApi(guid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://laniebla-vol2.onrender.com/api/auth/usuarios/$guid")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(response)

                    val nombre = jsonResponse.optString("nombre", "No definido")
                    val apellidos = jsonResponse.optString("apellidos", "No definido")
                    val usuario = jsonResponse.optString("usuario", "No definido")
                    val correo = jsonResponse.optString("correo", "No definido")
                    val telefono = jsonResponse.optString("telefono", "No definido")
                    val domicilio = jsonResponse.optString("domicilio", "No definido")
                    val familiar = jsonResponse.optString("familiar", "No definido")
                    val preguntaSecreta = jsonResponse.optString("preguntaSecreta", "")

                    withContext(Dispatchers.Main) {
                        txtNombre.text = "Nombre: $nombre"
                        txtApellidos.text = "Apellidos: $apellidos"
                        txtUsuario.text = "Usuario: $usuario"
                        txtCorreo.text = "Correo: $correo"
                        txtTelefono.text = "Teléfono: $telefono"
                        txtDomicilio.text = "Domicilio: $domicilio"
                        txtFamiliar.text = "Familiar: $familiar"
                        txtPreguntaSecreta.text = if(preguntaSecreta.isNotEmpty()) "Pregunta secreta: $preguntaSecreta" else ""
                    }
                } else {
                    Log.e("UserDetailActivity", "Error en respuesta: $responseCode")
                    withContext(Dispatchers.Main) {
                        txtNombre.text = "Error cargando datos"
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e("UserDetailActivity", "Error en la API: ${e.message}")
                withContext(Dispatchers.Main) {
                    txtNombre.text = "Error cargando datos"
                }
            }
        }
    }
}
