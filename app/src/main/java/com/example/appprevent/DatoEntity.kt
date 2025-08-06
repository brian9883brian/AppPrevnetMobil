package com.example.appprevent
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "datos")
data class DatoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mensaje: String
)
//comentario para subir xd