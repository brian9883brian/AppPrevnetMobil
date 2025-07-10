package com.example.appprevent

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Callback
import retrofit2.Response

interface ApiServiceNube {
    @POST("api/alertas")
    fun enviarTodosRegistros(): Call<Void>
}
