package com.example.retrofittodo

import retrofit2.Response
import retrofit2.http.GET

interface TodoApi {
    // Network Model Class (A data class) shows what the response looks like

    // For a large number of json attributes just copy the json response of single todo item
    // and use the json to kotlin class plugin (kotlin data class from json plugin option)

    // Put query parameters as parameters to function

    @GET("/todos")
    suspend fun getTodos() : Response<List<Todo>>
}