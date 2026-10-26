package arizatus.zahman.sanitasi.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Karena tadi kita sudah set webservicenya pakai Laravel, pastikan URL-nya mengarah ke public/
    private const val BASE_URL = "http://10.131.131.225/web-sanitasi/public/api/"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}