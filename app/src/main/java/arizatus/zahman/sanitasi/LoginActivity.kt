package arizatus.zahman.sanitasi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import arizatus.zahman.sanitasi.api.LoginResponse
import arizatus.zahman.sanitasi.api.ApiService
import arizatus.zahman.sanitasi.utils.LocationHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginActivity : AppCompatActivity() {

    private lateinit var tvLoginError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val etUsername = findViewById<EditText>(R.id.et_username)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val btnLogin = findViewById<Button>(R.id.btn_login)

        tvLoginError = findViewById(R.id.tv_login_error)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                tvLoginError.text = "Username dan password wajib diisi!"
                tvLoginError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            prosesLoginKeServer(username, password)
        }
    }

    private fun prosesLoginKeServer(username: String, password: String) {
        tvLoginError.visibility = View.GONE

        try {
            val retrofitDarurat = Retrofit.Builder()
                .baseUrl("http://10.125.150.225:8000/api/") // IP fisik laptopmu saat ini
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            // Membuat instance ApiService secara dinamis
            val apiServiceDarurat = retrofitDarurat.create(ApiService::class.java)

            // Eksekusi login tanpa melibatkan file RetrofitClient lama
            apiServiceDarurat.loginUser(username, password).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val loginBody = response.body()

                        if (loginBody?.status == "success") {
                            val sharedPref = getSharedPreferences("SanitasiSession", Context.MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putInt("USER_ID", loginBody.user?.id ?: 0)
                                stringPref(this, "USER_NAME", loginBody.user?.name)
                                stringPref(this, "USER_ROLE", loginBody.user?.role)
                                stringPref(this, "USER_KELURAHAN", loginBody.user?.kelurahan)
                                apply()
                            }

                            Toast.makeText(this@LoginActivity, "Selamat Datang, ${loginBody.user?.name}!", Toast.LENGTH_SHORT).show()
                            
                            LocationHelper.fetchDynamicJalan {
                                startActivity(Intent(this@LoginActivity, PendataanRumahTanggaActivity::class.java))
                                finish()
                            }
                        } else {
                            tvLoginError.text = loginBody?.message ?: "Login gagal, username/password salah."
                            tvLoginError.visibility = View.VISIBLE
                        }
                    } else {
                        tvLoginError.text = "Login gagal (Code: ${response.code()})"
                        tvLoginError.visibility = View.VISIBLE
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    // ⚠️ SEKARANG LOG ERROR DI LAYAR PASTI AKAN MEMBACA IP .184
                    tvLoginError.text = "Gagal terhubung ke Laragon: ${t.localizedMessage}"
                    tvLoginError.visibility = View.VISIBLE
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            tvLoginError.text = "Error Inisialisasi: ${e.message}"
            tvLoginError.visibility = View.VISIBLE
        }
    }

    // Helper untuk null-safety String SharedPreferences
    private fun stringPref(editor: android.content.SharedPreferences.Editor, key: String, value: Any?) {
        editor.putString(key, value?.toString() ?: "")
    }
}