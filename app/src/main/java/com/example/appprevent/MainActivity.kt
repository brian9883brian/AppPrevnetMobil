package com.example.appprevent
import java.net.URL
import java.net.HttpURLConnection
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), MessageClient.OnMessageReceivedListener {

    private lateinit var rvDatos: RecyclerView
    private lateinit var adapter: DatoAdapter
    private val datosRecibidos = mutableListOf<String>()
    private var guid: String = ""

    private lateinit var db: AppDatabase
    private lateinit var datoDao: DatoDao

    private lateinit var tvNombreUsuario: TextView

    // Buffer temporal para acumular mensajes antes de guardar
    private val bufferDatos = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "app_database"
        ).build()
        datoDao = db.datoDao()

        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val nombre = prefs.getString("usuario_nombre", "Usuario") ?: "Usuario"

        // Recibir guid del intent
        val guid = intent.getStringExtra("guid") ?: prefs.getString("usuario_guid", "") ?: ""

        // Guardar guid en SharedPreferences para futuras sesiones
        with(prefs.edit()) {
            putString("usuario_guid", guid)
            apply()
        }

        tvNombreUsuario = findViewById(R.id.tvNombreUsuario)
        tvNombreUsuario.text = "Hola, $nombre"

        // Mostrar guid en TextView (opcional, crea uno en tu layout con id tvGuid)
        val tvGuid = findViewById<TextView?>(R.id.tvGuid)
        tvGuid?.text = "GUID: $guid"

        rvDatos = findViewById(R.id.rvDatos)
        adapter = DatoAdapter(datosRecibidos)
        rvDatos.layoutManager = LinearLayoutManager(this)
        rvDatos.adapter = adapter

        cargarDatosDesdeBD()

        val btnBano = findViewById<MaterialButton>(R.id.btnBano)
        val btnTerminar = findViewById<MaterialButton>(R.id.btnTerminar)
        val btnComida = findViewById<MaterialButton>(R.id.btnComida)

        val buttonClickListener = { buttonText: String ->
            Toast.makeText(this, buttonText, Toast.LENGTH_SHORT).show()

            lifecycleScope.launch(Dispatchers.IO) {
                // Primero agrega el botón al buffer
                bufferDatos.add(buttonText)

                val copiaBuffer = bufferDatos.toList()
                val bufferInsertExito = insertarBufferEnDB()

                var botonInsertExito = false
                try {
                    datoDao.insertarDato(DatoEntity(mensaje = buttonText))
                    botonInsertExito = true
                } catch (e: Exception) {
                    Log.e("DB", "Error al insertar dato botón: ${e.message}")
                }

                Log.d("API", "Buffer antes de enviar: $copiaBuffer")
                enviarDatosAlAPI(copiaBuffer)  // Enviar el buffer completo, ya con el botón incluido

                launch(Dispatchers.Main) {
                    if (bufferInsertExito && botonInsertExito) {
                        Toast.makeText(this@MainActivity, "Datos guardados correctamente", Toast.LENGTH_SHORT).show()
                        Log.d("DB", "Buffer y botón insertados correctamente")
                    } else if (!bufferInsertExito && !botonInsertExito) {
                        Toast.makeText(this@MainActivity, "Error al guardar datos (buffer y botón)", Toast.LENGTH_LONG).show()
                    } else if (!bufferInsertExito) {
                        Toast.makeText(this@MainActivity, "Error al guardar datos del buffer", Toast.LENGTH_LONG).show()
                    } else if (!botonInsertExito) {
                        Toast.makeText(this@MainActivity, "Error al guardar dato del botón", Toast.LENGTH_LONG).show()
                    }
                }
            }

            runOnUiThread {
                datosRecibidos.clear()
                datosRecibidos.add(buttonText)
                adapter.notifyDataSetChanged()
                rvDatos.scrollToPosition(datosRecibidos.size - 1)
            }
        }




        btnBano.setOnClickListener { buttonClickListener(btnBano.text.toString()) }
        btnTerminar.setOnClickListener { buttonClickListener(btnTerminar.text.toString()) }
        btnComida.setOnClickListener { buttonClickListener(btnComida.text.toString()) }

        Wearable.getMessageClient(this).addListener(this)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val headerView = navView.getHeaderView(0)
        val tvNombreUsuarioHeader = headerView.findViewById<TextView>(R.id.tvNombreUsuario)
        tvNombreUsuarioHeader.text = nombre

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_inicio -> startActivity(Intent(this, MainActivity::class.java))
                R.id.nav_vista1 -> startActivity(Intent(this, HistoricoActivity::class.java))
                R.id.nav_vista2 -> startActivity(Intent(this, UserDetailActivity::class.java))
            }
            drawerLayout.closeDrawers()
            true
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        val mensaje = String(event.data)
        Log.d("PhoneApp", "Mensaje recibido: $mensaje")

        val intent = Intent("com.example.appprevent.ACTUALIZAR_HISTORICO")
        intent.putExtra("mensaje", mensaje)
        sendBroadcast(intent)

        // Solo agregar al buffer y a UI, NO insertar todavía
        bufferDatos.add(mensaje)

        runOnUiThread {
            datosRecibidos.add(mensaje)
            adapter.notifyItemInserted(datosRecibidos.size - 1)
            rvDatos.scrollToPosition(datosRecibidos.size - 1)
        }
    }

    private suspend fun insertarBufferEnDB(): Boolean {
        if (bufferDatos.isEmpty()) return true // No hay nada que insertar, no es error
        return try {
            val copiaBuffer = bufferDatos.toList()
            val listaEntidad = copiaBuffer.map { DatoEntity(mensaje = it) }
            datoDao.insertarDatos(listaEntidad)
            bufferDatos.clear()
            Log.d("DB", "Inserción batch exitosa, ${listaEntidad.size} elementos guardados")
            true
        } catch (e: Exception) {
            Log.e("DB", "Error al insertar datos: ${e.message}")
            false
        }
    }
    private fun enviarDatosAlAPI(datos: List<String>) {
        Log.d("API", "Simulando envío de datos: $datos")

        val json = datos.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
        Log.d("API", "Simulando JSON a enviar: $json")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Thread.sleep(500) // Simulación de tiempo de espera al enviar
                Log.d("API", "Simulación de envío completada exitosamente")
            } catch (e: InterruptedException) {
                Log.e("API", "Simulación interrumpida: ${e.message}")
            }
        }
    }


    private fun cargarDatosDesdeBD() {
        lifecycleScope.launch(Dispatchers.IO) {
            val datosEnBD = datoDao.obtenerDatos()
            val mensajes = datosEnBD.map { it.mensaje }
            runOnUiThread {
                datosRecibidos.clear()
                datosRecibidos.addAll(mensajes)
                adapter.notifyDataSetChanged()
                if (mensajes.isNotEmpty()) {
                    rvDatos.scrollToPosition(mensajes.size - 1)
                }
            }
        }
    }

    override fun onDestroy() {
        lifecycleScope.launch(Dispatchers.IO) {
            insertarBufferEnDB() // insertar lo que quede pendiente
        }
        super.onDestroy()
        Wearable.getMessageClient(this).removeListener(this)
    }
}
