package com.example.appprevent

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("datos/")
    fun enviarDato(@Body dato: DatoRequest): Call<DatoRequest>
}
